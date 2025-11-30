package util.data.procs;

import org.tinylog.Logger;
import util.math.MathUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Reducer {

    public static DoubleArrayToDouble getDoubleReducer(String reducer, double defValue, int windowsize, int scaleParam) {
        return switch (reducer.replace(" ", "").toLowerCase()) {
            case "mean", "avg" -> (window) -> Arrays.stream(window).average().orElse(defValue);
            case "median" -> {
                if (windowsize % 2 == 0) {
                    yield (window) -> {
                        var sorted = DoubleStream.of(window).sorted().toArray();
                        return sorted[sorted.length / 2];
                    };
                } else {
                    yield (window) -> {
                        var sorted = DoubleStream.of(window).sorted().toArray();
                        return (sorted[sorted.length / 2] + sorted[sorted.length / 2 - 1]) / 2;
                    };
                }

            }
            case "sumofsquares" -> MathUtils::calcSumOfSquares;
            case "variance", "samplevariance" -> (window) -> MathUtils.calcSumOfSquares(window) / (window.length - 1);
            case "populationvariance","popvariance","popvar" -> (window) -> MathUtils.calcSumOfSquares(window) / (window.length);
            case "stdev", "standarddeviation" -> (window) -> Math.sqrt(MathUtils.calcSumOfSquares(window) / (window.length - 1));
            case "popstdev", "populationstandarddeviation" -> (window) -> Math.sqrt(MathUtils.calcSumOfSquares(window) / window.length);
            case "mode" -> (window) -> {
                int scale = scaleParam;
                if ( scale == -1 ) {
                    // System figures it out: find max decimal places in the dataset
                    scale = Arrays.stream(window)
                            .mapToInt(d -> {
                                String s = String.valueOf(d);
                                int decimalIndex = s.indexOf('.');
                                return (decimalIndex == -1) ? 0 : s.length() - decimalIndex - 1;
                            })
                            .max()
                            .orElse(0);

                    scale = (int) Math.pow(10, scale); // Convert decimal places to scale factor
                }
                int finalScale = scale;
                //int scale = 100; // Adjust for desired precision (e.g., 2 decimal places)
                Map<Integer, Long> frequencyMap = Arrays.stream(window)
                        .mapToInt(d -> (int) (d * finalScale))
                        .boxed()
                        .collect(Collectors.groupingBy(i -> i, Collectors.counting()));

                return frequencyMap.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(e -> e.getKey() / (double) finalScale)
                        .orElse(defValue);
            };
            case "max" -> (window) -> {
                var max = Double.MIN_VALUE;
                for (var a : window)
                    max = Math.max(max, a);
                return max;
            };
            case "min" -> (window) -> {
                var min = Double.MAX_VALUE;
                for (var a : window)
                    min = Math.min(min, a);
                return min;
            };
            case "sum" -> sumDoubles();
            default -> {
                Logger.warn("Unknown reducer type '{}'. Returning null. Waiting on your pull request to get it implemented!", reducer);
                yield null;
            }

        };
    }
    public static boolean isValidIntegerReducer(String reducer ){
        return reducer.equals("max")||reducer.equals("min")||reducer.equals("sum");
    }
    public static IntegerArrayToInteger getIntegerReducer(String reducer, int defValue, int windowsize) {
        return switch (reducer.replace(" ", "").toLowerCase()) {
            case "max" -> (window) -> {
                var max = Integer.MIN_VALUE;
                for (var a : window)
                    max = Math.max(max, a);
                return max;
            };
            case "min" -> (window) -> {
                var min = Integer.MAX_VALUE;
                for (var a : window)
                    min = Math.min(min, a);
                return min;
            };
            case "sum" -> sumInts();
            default -> {
                Logger.warn("Unknown reducer type '{}'. Returning null. Waiting on your pull request to get it implemented!", reducer);
                yield null;
            }

        };
    }

    private static IntegerArrayToInteger sumInts() {
        return (window) -> {
            var sum = 0;
            for (var a : window)
                sum += a;
            return sum;
        };
    }
    private static DoubleArrayToDouble sumDoubles() {
        return (window) -> {
            double sum = 0;
            for (var a : window)
                sum += a;
            return sum;
        };
    }
}
