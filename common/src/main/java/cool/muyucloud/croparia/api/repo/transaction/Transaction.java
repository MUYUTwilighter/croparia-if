package cool.muyucloud.croparia.api.repo.transaction;

import cool.muyucloud.croparia.CropariaIf;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified Transaction API for Repo API.
 */
public interface Transaction extends AutoCloseable {
    /**
     * Open a root (base) transaction
     */
    static Transaction open() {
        return new Base();
    }

    /**
     * Open a nested transaction
     */
    Transaction nest() throws IllegalStateException;

    /**
     * Add an event task that is executed during base transaction rolling back.
     */
    void onRollback(Task task) throws IllegalStateException;

    /**
     * Add an event task that is executed during base transaction succeeding.
     */
    void onFinish(Task task) throws IllegalStateException;

    /**
     * Add an event task that is executed when current transaction is closing.
     */
    void onClose(Task task) throws IllegalStateException;

    void commit() throws IllegalStateException;

    @Override
    void close() throws IllegalStateException;

    interface Task extends Runnable {
        @Override
        void run() throws RuntimeException;

        default void guard() {
            try {
                this.run();
            } catch (RuntimeException e) {
                CropariaIf.LOGGER.error("Caught an exception while trying to run task", e);
            }
        }
    }

    class Base implements Transaction {
        private boolean childActive = false;
        private boolean closed = false;
        private boolean committed = false;
        protected final List<Task> onClose = new ArrayList<>();
        protected final List<Task> onFinish = new ArrayList<>();
        protected final List<Task> onRollback = new ArrayList<>();

        protected Base() {
        }

        @Override
        public void commit() throws IllegalStateException {
            this.throwIfClosed();
            this.throwIfCommitted();
            this.throwIfChildActive();
            this.markCommited();
        }

        @Override
        public void close() throws IllegalStateException {
            this.throwIfClosed();
            this.throwIfChildActive();
            this.markClosed();
            if (this.isCommitted()) this.onFinish.forEach(Task::guard);
            else this.onRollback.reversed().forEach(Task::guard);
            this.onClose.forEach(Task::guard);
        }

        @Override
        public void onRollback(Task task) {
            this.throwIfClosed();
            this.throwIfCommitted();
            this.throwIfChildActive();
            this.onRollback.add(task);
        }

        @Override
        public void onFinish(Task task) throws IllegalStateException {
            this.throwIfClosed();
            this.throwIfCommitted();
            this.throwIfChildActive();
            this.onFinish.add(task);
        }

        @Override
        public void onClose(Task task) throws IllegalStateException {
            this.throwIfClosed();
            this.throwIfCommitted();
            this.throwIfChildActive();
            this.onClose.add(task);
        }

        public void throwIfCommitted() throws IllegalStateException {
            if (this.isCommitted()) throw new IllegalStateException("Transaction is closed");
        }

        public void throwIfClosed() throws IllegalStateException {
            if (this.isClosed()) throw new IllegalStateException("Transaction is closed");
        }

        public void throwIfChildActive() throws IllegalStateException {
            if (this.isChildActive()) throw new IllegalStateException("A child transaction is active");
        }

        public boolean isCommitted() {
            return committed;
        }

        protected void markCommited() {
            this.committed = true;
        }

        public boolean isClosed() {
            return closed;
        }

        protected void markClosed() {
            this.closed = true;
        }

        public boolean isChildActive() {
            return childActive;
        }

        protected void markChildActive(boolean childActive) {
            this.childActive = childActive;
        }

        @Override
        public Transaction nest() throws IllegalStateException {
            this.throwIfClosed();
            this.throwIfCommitted();
            this.throwIfChildActive();
            Nested nested = new Nested(this);
            nested.onClose(() -> this.markChildActive(false));
            this.markChildActive(true);
            return nested;
        }
    }

    class Nested extends Base {
        private final @NotNull Base parent;

        protected Nested(final @NotNull Base parent) throws IllegalStateException {
            parent.throwIfChildActive();
            parent.throwIfClosed();
            parent.throwIfCommitted();
            this.parent = parent;
        }

        public @NotNull Base getParent() {
            return parent;
        }

        @Override
        public void close() throws IllegalStateException {
            this.throwIfClosed();
            this.throwIfChildActive();
            this.markClosed();
            if (this.isCommitted()) {
                // direct addition to parent task list to avoid #childActive check
                this.getParent().onFinish.addAll(this.onFinish);
                this.getParent().onRollback.addAll(this.onRollback);
            } else {
                this.onRollback.reversed().forEach(Task::guard);
            }
            this.onClose.forEach(Task::guard);
        }

        @Override
        public void throwIfClosed() throws IllegalStateException {
            super.throwIfClosed();
            this.getParent().throwIfClosed();
        }

        @Override
        public void throwIfCommitted() throws IllegalStateException {
            super.throwIfCommitted();
            this.getParent().throwIfClosed();
        }
    }
}
