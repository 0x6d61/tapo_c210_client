package io.github.tapo.c210.application;

/** Handle returned by a camera connection adapter. */
public interface ConnectedCamera extends AutoCloseable {
    @Override
    void close() throws Exception;
}
