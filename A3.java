import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class A3 {
    // regex for all emails
    //public static final String EMAILS_REGEX = "(?i)((From|To|Cc|Bcc):\\s*\\b[A-Za-z0-9._%+-]+" +
    //        "@[A-Za-z0-9.-]+\\.[A-Z]{2,}\\b|\\b[A-Za-z0-9._%+-]+@enron\\.com\\b)";
    public static final String EMAILS_REGEX = "(?i)((From|To|Cc|Bcc):\\s*\\b[A-Za-z0-9._%+-]+" +
            "@enron\\.com\\b|\\b[A-Za-z0-9._%+-]+@enron\\.com\\b)";
    // pattern for all emails
    public static final Pattern EMAILS_PATTERN = Pattern.compile(EMAILS_REGEX);
    // regex for email
    public static final String EMAIL_REGEX = "\\b[A-Za-z0-9._%+-]+@enron\\.com\\b";
    // pattern for email
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    // processed file count
    private static int processedFileCount = 0;

    public static void main(String[] args) {
        // check command line arguments
        if (args.length < 1) {
            System.err.println("Usage: java A3 <mail directory> [connectors file]");
            return;
        }

        // check mail data dir
        Path root = Paths.get(args[0]);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            System.err.println("Invalid data dir: " + args[0]);
            return;
        }

        // check connector files path
        Path filePath = null;
        if (args.length > 1) {
            filePath = Paths.get(args[1]);
        }

        // create graph
        Graph graph = new Graph();
        // read data to graph
        readData(graph, root);

        // calculate connectors and teams
        graph.calculate();
        // show connectors
        showConnectors(graph, filePath);
        // process input from user
        process(graph);
    }

    /**
     * Read data from root dir to graph
     *
     * @param graph the graph
     * @param root  the root dir
     */
    private static void readData(Graph graph, Path root) {
        // queue for BSF travel dirs
        final Queue<Path> queue = new LinkedList<>();
        queue.add(root);

        // create mutex and thread pool for multi-thread process mail files
        ReentrantLock mutex = new ReentrantLock();
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        System.out.println("Reading mail files in " + root + "...");

        // loop until all dirs processed
        while (!queue.isEmpty()) {
            Path dirPath = queue.poll();

            // travel dir
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                // process all path
                for (Path path : stream) {
                    // path is dir, add to queue
                    if (Files.isDirectory(path)) {
                        queue.add(path);
                        continue;
                    }

                    // path is file, read file
                    if (Files.isRegularFile(path)) {
                        // readFile(graph, path, mutex);
                        // create task
                        Runnable task = () -> readFile(graph, path, mutex);
                        // run task in thread pool
                        pool.execute(task);
                    }
                }

            } catch (IOException e) {
                System.err.println("Process '" + dirPath + "' failed: " + e.getMessage());
            }
        }

        // wait for thread pool down
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Read " + processedFileCount + " mail files");
    }

    /**
     * Read file data to graph
     *
     * @param graph    the graph
     * @param filePath the file path
     */
    private static void readFile(Graph graph, Path filePath, ReentrantLock mutex) {
        // read file
        try {
            mutex.lock();
            ++processedFileCount;
            mutex.unlock();

            // from email address
            String from = null;

            // read all content
            String content = Files.readString(filePath, StandardCharsets.ISO_8859_1);
            // find email using regex
            Matcher matcher = EMAILS_PATTERN.matcher(content);

            // process all emails
            while (matcher.find()) {
                String str = matcher.group();
                // find email address
                Matcher emailMatcher = EMAIL_PATTERN.matcher(str);
                if (!emailMatcher.find()) {
                    continue;
                }
                String email = emailMatcher.group();

                // find from email address
                if (from == null && str.startsWith("From:")) {
                    from = email;
                    continue;
                }

                // no 'From:', skip
                if (from == null) {
                    // graph.addVertex(email);
                    continue;
                }

                // add edge to graph
                mutex.lock();
                graph.addEdge(from, email);
                mutex.unlock();
            }

        } catch (IOException e) {
            // throw new RuntimeException(e);
            System.err.println("Read '" + filePath + "' failed: " + e.getMessage());
        }
    }

    /**
     * Show connectors
     *
     * @param graph    the graph
     * @param filePath the connectors output file path
     */
    private static void showConnectors(Graph graph, Path filePath) {
        // get all connectors
        Set<String> connectors = graph.getConnectors();

        // show to stdout
        System.out.println("\nConnectors:");
        for (String connector : connectors) {
            System.out.println(connector);
        }
        System.out.println();

        // no file to output
        if (filePath == null) {
            return;
        }

        // write all connectors to output file
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (String connector : connectors) {
                writer.write(connector);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Process user input
     *
     * @param graph the graph
     */
    private static void process(Graph graph) {
        // read from stdin
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("Email address of the individual (or EXIT to quit): ");
                // get input
                String input = reader.readLine();

                // input is null, done
                if (input == null) {
                    break;
                }

                // input is EXIT, done
                input = input.trim();
                if (input.equalsIgnoreCase("EXIT")) {
                    break;
                }

                // no such email in graph
                if (!graph.hasVertex(input)) {
                    System.out.println("Email address (" + input + ") not found in the dataset.");
                    continue;
                }

                // show email information
                System.out.println("* " + input + " has sent messages to " + graph.sentIndividualCount(input) + " others");
                System.out.println("* " + input + " has received messages from " + graph.receivedIndividualCount(input) + " others");
                System.out.println("* " + input + " is in a team with " + graph.teamMemberCount(input) + " individuals");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
