package ch.puzzle.lightning.minizeus.invoices.entity;

public class ZeusInternalException extends RuntimeException {
    public ZeusInternalException(String message) {
        super(message);
    }

    public ZeusInternalException(Exception e) {
        super(e);
    }
}
