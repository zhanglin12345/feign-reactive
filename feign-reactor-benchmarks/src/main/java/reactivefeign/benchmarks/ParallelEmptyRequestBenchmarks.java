package reactivefeign.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 3, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ParallelEmptyRequestBenchmarks extends RealRequestBenchmarks{

  public static final int CALLS_NUMBER = 100;
  private ExecutorService executor;

  @Setup
  public void setup() throws Exception {
    super.setup();
    executor = Executors.newFixedThreadPool(CALLS_NUMBER);
  }

  @TearDown
  public void tearDown() throws Exception {
    super.tearDown();
    executor.shutdown();
  }

  //NO PAYLOAD

  @Benchmark
  public void feignEmptyPayload() throws ExecutionException, InterruptedException {

    CompletableFuture[] bonusesCompletableFutures = IntStream.range(0, CALLS_NUMBER)
            .mapToObj(runnable -> CompletableFuture.runAsync(() -> feign.justGet(), executor))
            .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(bonusesCompletableFutures).get();
  }

  /**
   * How fast can we execute get commands synchronously using reactive web client based Feign?
   */
  @Benchmark
  public void webClient() {

    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> webClientFeign.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void jetty() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> jettyFeign.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void jettyH2c() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> jettyFeignH2c.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void java11() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> java11Feign.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  @Benchmark
  public void java11H2c() {
    Mono.zip(IntStream.range(0, CALLS_NUMBER)
                    .mapToObj(i -> java11FeignH2c.justGet())
                    .collect(Collectors.toList()),
            values -> values).block();
  }

  //used to run from IDE
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
            .jvmArgs("-Xms1024m", "-Xmx4024m")
            .include(".*" + ParallelEmptyRequestBenchmarks.class.getSimpleName() + ".*")
            //.addProfiler( StackProfiler.class )
            .build();

    new Runner(opt).run();
  }

}
