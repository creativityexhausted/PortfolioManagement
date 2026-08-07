from flask import Flask, request, jsonify
from flask_cors import CORS
import numpy as np

from qiskit.primitives import Sampler
from qiskit_algorithms import QAOA
from qiskit_algorithms.optimizers import COBYLA
from qiskit_optimization import QuadraticProgram
from qiskit_optimization.algorithms import MinimumEigenOptimizer
from qiskit_optimization.converters import QuadraticProgramToQubo

app = Flask(__name__)
CORS(app)

@app.route('/api/optimize', methods=['POST'])
def optimize_portfolio():
    data = request.json
    assets = data.get('assets', [])
    # Default budget is half of the assets
    budget = data.get('budget', len(assets) // 2)
    risk_factor = data.get('riskFactor', 0.5)

    if not assets:
        return jsonify({"error": "No assets provided"}), 400

    n_assets = len(assets)
    if budget >= n_assets:
        budget = n_assets - 1
    if budget <= 0:
        budget = 1
        
    # 1. Generate synthetic expected returns (mu) and covariance matrix (sigma)
    # Using a fixed seed so the same assets yield the same results for demo purposes
    np.random.seed(len(assets))
    mu = np.random.uniform(0.01, 0.15, n_assets)
    
    # Create a random positive semi-definite matrix for covariance
    A = np.random.rand(n_assets, n_assets)
    sigma = np.dot(A, A.transpose()) * 0.05
    
    # 2. Formulate the Portfolio Optimization Problem
    qp = QuadraticProgram()
    
    # Add binary variables for each asset (1 = include, 0 = exclude)
    for i in range(n_assets):
        qp.binary_var(name=f"x_{i}")
        
    # Objective function: minimize (risk_factor * variance - expected_return)
    linear = {}
    quadratic = {}
    
    for i in range(n_assets):
        linear[f"x_{i}"] = -mu[i]
        for j in range(n_assets):
            quadratic[(f"x_{i}", f"x_{j}")] = risk_factor * sigma[i, j]
            
    qp.minimize(linear=linear, quadratic=quadratic)
    
    # Budget constraint: select exactly 'budget' number of assets
    linear_constraint = {f"x_{i}": 1 for i in range(n_assets)}
    qp.linear_constraint(linear=linear_constraint, sense="==", rhs=budget, name="budget_constraint")
    
    # Convert constraints to penalties (QUBO formulation)
    # Penalty needs to be large enough to enforce the constraint
    penalty_factor = max(np.abs(mu)) * 10
    conv = QuadraticProgramToQubo(penalty=penalty_factor)
    qubo = conv.convert(qp)
    
    # 3. Quantum Optimization (QAOA)
    try:
        # We use a local statevector simulator via the Sampler primitive
        sampler = Sampler()
        # COBYLA is a good classical optimizer for noisy/simulated quantum circuits
        optimizer = COBYLA(maxiter=100)
        qaoa = QAOA(sampler=sampler, optimizer=optimizer, reps=1)
        
        # MinimumEigenOptimizer translates the QUBO to an Ising Hamiltonian and solves it
        calc = MinimumEigenOptimizer(qaoa)
        result = calc.solve(qubo)
        
        # Extract the optimal binary string (e.g., [1.0, 0.0, 1.0])
        optimal_selection = [int(x) for x in result.x]
        
        # Calculate metrics for the selected portfolio
        selected_indices = [i for i, x in enumerate(optimal_selection) if x == 1]
        
        exp_return = 0
        risk = 0
        if len(selected_indices) > 0:
            exp_return = float(sum([mu[i] for i in selected_indices]))
            risk = float(sum([sigma[i, j] for i in selected_indices for j in selected_indices]))
            
        response = {
            "assets": assets,
            "optimalSelection": optimal_selection,
            "expectedReturn": exp_return,
            "risk": risk,
            "fval": float(result.fval),
            "status": "SUCCESS"
        }
        
        return jsonify(response)
        
    except Exception as e:
        return jsonify({"error": str(e), "status": "ERROR"}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
