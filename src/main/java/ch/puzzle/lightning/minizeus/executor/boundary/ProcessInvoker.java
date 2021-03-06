package ch.puzzle.lightning.minizeus.executor.boundary;

import ch.puzzle.lightning.minizeus.invoices.entity.Invoice;
import ch.puzzle.lightning.minizeus.invoices.entity.InvoiceSettled;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Logger;

@ApplicationScoped
public class ProcessInvoker {

    private static final Logger LOG = Logger.getLogger(ProcessInvoker.class.getName());

    @Inject
    @ConfigProperty(name = "app.exec.path")
    Optional<String> appExecPath;

    @Inject
    @ConfigProperty(name = "app.beertap.name", defaultValue = "FlashFlush")
    String memoPrefix;

    public void consumeInvoice(@ObservesAsync InvoiceSettled event) {
        LOG.info("consumeInvoice " + event.id);
        CompletableFuture.supplyAsync(() -> {
            try {
                if (appExecPath.filter(Predicate.not(String::isBlank)).isPresent()) {
                    return executeCommand(event);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return 10;
        }).whenComplete((code, t) -> {
            if (t != null) {
                t.printStackTrace();
                LOG.severe("Error while executing invocation");
            } else {
                LOG.info("return code " + code);
            }
        });
    }


    private Integer executeCommand(InvoiceSettled invoice) throws IOException, InterruptedException {
        String productsArg = "--products=" + getFromMemo(invoice.memo);
        if(!invoice.memo.startsWith(memoPrefix)) {
            LOG.info("Not a beerTap invoice");
            return 10;
        }

        LOG.info("Command: " + appExecPath + ", Args: " + productsArg);

        ProcessBuilder pb = new ProcessBuilder(appExecPath.orElse("echo"), productsArg);
        Map<String, String> env = pb.environment();
        pb.directory(Paths.get(".").toFile());
        Process p = pb.start();
        Integer returnCode = p.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        while ((line = reader.readLine()) != null) {
            LOG.info(line);
        }

        return returnCode;
    }

    private String getFromMemo(String memo) {
        return memo;
    }
}
