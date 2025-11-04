package io.github.koinsaari.jtoon.cli;

import io.github.koinsaari.jtoon.ToonOptions;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * CLI entry point for TOON encoder/decoder.
 * Usage: java -jar j-toon-cli.jar [options] [input]
 */
public class Main {
    public static void main(String[] args) {
        try {
            CliOptions options = parseArgs(args);
            String input = readInput(options);
            String output = CliConverter.convert(input, options);
            writeOutput(output, options);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (System.getenv("DEBUG") != null) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private static CliOptions parseArgs(String[] args) throws Exception {
        CliOptions options = new CliOptions();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-o") || arg.equals("--output")) {
                if (i + 1 >= args.length) {
                    throw new Exception("Missing value for " + arg);
                }
                options.output = args[++i];
            } else if (arg.equals("-e") || arg.equals("--encode")) {
                options.encode = true;
                options.decode = false;
            } else if (arg.equals("-d") || arg.equals("--decode")) {
                options.decode = true;
                options.encode = false;
            } else if (arg.equals("--delimiter")) {
                if (i + 1 >= args.length) {
                    throw new Exception("Missing value for --delimiter");
                }
                String delim = args[++i];
                options.delimiter = parseDelimiter(delim);
            } else if (arg.equals("--indent")) {
                if (i + 1 >= args.length) {
                    throw new Exception("Missing value for --indent");
                }
                try {
                    options.indent = Integer.parseInt(args[++i]);
                    if (options.indent < 1) {
                        throw new Exception("Indent must be >= 1");
                    }
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid indent value: " + args[i]);
                }
            } else if (arg.equals("--length-marker")) {
                options.lengthMarker = true;
            } else if (arg.equals("--stats")) {
                options.stats = true;
            } else if (arg.equals("--no-strict")) {
                options.strict = false;
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                System.exit(0);
            } else if (!arg.startsWith("-")) {
                if (options.input != null) {
                    throw new Exception("Multiple input files not supported");
                }
                options.input = arg;
            } else {
                throw new Exception("Unknown option: " + arg);
            }
        }

        return options;
    }

    private static ToonOptions.Delimiter parseDelimiter(String delim) throws Exception {
        return switch (delim) {
            case "," -> ToonOptions.Delimiter.COMMA;
            case "\\t", "\t" -> ToonOptions.Delimiter.TAB;
            case "|" -> ToonOptions.Delimiter.PIPE;
            default -> throw new Exception("Unknown delimiter: " + delim + ". Use: , | \\t");
        };
    }

    private static String readInput(CliOptions options) throws Exception {
        if (options.input == null || options.input.equals("-")) {
            return new String(System.in.readAllBytes());
        }
        return Files.readString(Paths.get(options.input));
    }

    private static void writeOutput(String output, CliOptions options) throws Exception {
        if (options.output == null) {
            System.out.print(output);
        } else {
            Files.writeString(Paths.get(options.output), output);
        }
    }

    private static void printHelp() {
        System.out.println("""
            TOON Converter - Encode/Decode between JSON and TOON formats
            
            Usage: j-toon [options] [input]
            
            Arguments:
              input                Input file (default: stdin, use '-' for explicit stdin)
            
            Options:
              -o, --output FILE    Output file (default: stdout)
              -e, --encode         Force encode mode (JSON → TOON)
              -d, --decode         Force decode mode (TOON → JSON)
              --delimiter CHAR     Array delimiter: , | \\t (default: ,)
              --indent N           Indentation spaces (default: 2)
              --length-marker      Add # prefix to array lengths
              --stats              Show token count estimates (encode only)
              --no-strict          Disable strict validation (decode only)
              -h, --help           Show this help message
            
            Examples:
              j-toon data.json -o output.toon
              j-toon data.toon -o output.json
              cat data.json | j-toon --stats
              j-toon --delimiter \\t data.json > output.toon
            """);
    }
}
