package vn.t3tarot.api.identity.emailverification.config;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.DisposableBean;

@RequiredArgsConstructor
public class VerificationMailDispatcher implements DisposableBean {

    private final Executor executor;
    private final Runnable shutdownAction;

    public static VerificationMailDispatcher inline() {
        return new VerificationMailDispatcher(Runnable::run, () -> { });
    }

    public void dispatch(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void destroy() {
        shutdownAction.run();
    }
}
