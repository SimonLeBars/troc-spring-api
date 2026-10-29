package fr.cours.troc.trade;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// Séance 4 : la journalisation des trocs, écrite une fois, hors de TradeService.
@Aspect
@Component
public class TradeLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(TradeLoggingAspect.class);

    // Toutes les méthodes publiques de TradeService, quels que soient le retour et les arguments.
    @Pointcut("execution(* fr.cours.troc.trade.TradeService.*(..))")
    void tradeOperations() {
    }

    @Before("tradeOperations()")
    public void logAttempt(JoinPoint jp) {
        log.info("→ {} {}", jp.getSignature().getName(), Arrays.toString(jp.getArgs()));
    }

    @AfterReturning(pointcut = "tradeOperations()", returning = "result")
    public void logSuccess(JoinPoint jp, Object result) {
        // findAll renvoie une liste : on ne détaille que les méthodes qui renvoient un troc
        if (result instanceof Trade trade) {
            log.info("✓ {} : troc {} {}", jp.getSignature().getName(), trade.getId(), trade.getStatus());
        } else {
            log.info("✓ {}", jp.getSignature().getName());
        }
    }

    @AfterThrowing(pointcut = "tradeOperations()", throwing = "ex")
    public void logFailure(JoinPoint jp, RuntimeException ex) {
        log.warn("✗ {} : {}", jp.getSignature().getName(), ex.getMessage());
    }
}
