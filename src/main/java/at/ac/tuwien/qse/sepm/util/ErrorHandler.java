package at.ac.tuwien.qse.sepm.util;

/**
 * Handler used to handle exceptions at a different location than the parent frame in the call stack.
 */
public interface ErrorHandler<E extends Throwable> {

    /**
     * Notify the consumer about the exception.
     * @param exception Exception to present to the user of the error handler.
     */
    default void propagate(E exception) {
        handle(exception);
    }

    /**
     * Handle an occuring exception.
     * @param exception Excepiton to be handled.
     */
    void handle(E exception);
}