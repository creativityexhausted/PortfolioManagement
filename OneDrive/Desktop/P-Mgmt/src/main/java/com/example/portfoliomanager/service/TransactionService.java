package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.Transaction;
import com.example.portfoliomanager.dto.ApiDtos.TransactionRequest;
import com.example.portfoliomanager.dto.ApiDtos.TransactionResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository repository;
    private final PortfolioService portfolioService;

    public TransactionService(TransactionRepository repository, PortfolioService portfolioService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll(Long portfolioId) {
        List<Transaction> transactions = portfolioId == null
                ? repository.findAll()
                : repository.findByPortfolioId(portfolioId);
        return transactions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public TransactionResponse create(TransactionRequest request) {
        Transaction transaction = new Transaction();
        apply(transaction, request);
        return toResponse(repository.save(transaction));
    }

    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = getEntity(id);
        apply(transaction, request);
        return toResponse(repository.save(transaction));
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Transaction getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private void apply(Transaction transaction, TransactionRequest request) {
        transaction.setType(request.type());
        transaction.setSymbol(request.symbol().trim().toUpperCase(Locale.ROOT));
        transaction.setQuantity(request.quantity());
        transaction.setPricePerShare(request.pricePerShare());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setNotes(request.notes());
        transaction.setPortfolio(portfolioService.getEntity(request.portfolioId()));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getSymbol(),
                transaction.getQuantity(), transaction.getPricePerShare(), transaction.getTransactionDate(),
                transaction.getNotes(), transaction.getPortfolio().getId());
    }
}
