package org.fscotto.demo;

import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemorySegment;

import java.nio.ByteOrder;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class DemoRunner {

    public static void main(String[] args) {
        doSwitchExpressionDemo();
        doInstanceOfPatternMatching();
        doTextBlockDemo();
        doForeignMemoryAccessApiDemo();
        doRecordDemo();
    }

    private static void doSwitchExpressionDemo() {
        final var weekEndResult = isWeekEnd(LocalDate.now().getDayOfWeek());
        System.out.println(weekEndResult);
    }

    private static boolean isWeekEnd(final DayOfWeek day) {
        return switch (day) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
    }

    private static void doInstanceOfPatternMatching() {
        printValueType("hello");
        printValueType(10);
        printValueType(.0);
    }

    private static void printValueType(final Object o) {
        if (o instanceof String s) {
            System.out.println("It is string = " + s);
        } else if (o instanceof Integer i) {
            System.out.println("It is integer = " + i);
        } else if (o instanceof Double d) {
            System.out.println("It is double = " + d);
        }
    }

    private static void doTextBlockDemo() {
        final var sql = """
                select *\s\
                from users;
                """;
        System.out.println(sql);

        final var person = """
                {
                  "name": "$name",
                  "surname": "$surname",
                  "favourite_films": [
                    {
                      "title": "Harry Potter"
                    },
                    {
                      "title": "Star Wars"
                    }
                  ]
                }
                """
                .replace("$name", "Fabio")
                .replace("$surname", "Scotto di Santolo");
        System.out.println(person);
    }

    private static void doForeignMemoryAccessApiDemo() {
        final var real = MemoryLayout.ofValueBits(64, ByteOrder.BIG_ENDIAN);
        final var complex = MemoryLayout.ofStruct(
                real.withName("real"),
                real.withName("image")
        );

        final var shape = MemoryLayout.ofSequence(128, complex);
        final var bs = shape.varHandle(long.class,
                PathElement.sequenceElement(),
                PathElement.groupElement("image")
        );

        try (final var segment = MemorySegment.allocateNative(shape)) {
            for (int i = 0; i < shape.elementCount().orElse(0); i++) {
               final long b = (Long) bs.get(segment.baseAddress(), i);
               bs.set(segment.baseAddress(), i, b + 1);
            }
        }
    }

    private static void doRecordDemo() {
        final var mm = new MinMax(20, 22);
        System.out.println(mm);

        final var strings = List.of("hello", "world", "I", "am", "Vergingetorige");
        final var longestString = longest(strings);
        longestString.ifPresent(System.out::println);

        final var gv = new GenericValue<Integer>(10, 0);
        System.out.println(gv);
    }

    final static record MinMax(int min, int max) {

        static {
            System.out.println("record's static initialization block");
        }

        public MinMax {
            if (min > max)
                throw new IllegalArgumentException();
        }

        public static MinMax zero() {
            return new MinMax(0, 0);
        }

        public int avarege() {
            return (min + max) / 2;
        }

    }

    private static <T extends CharSequence> Optional<T> longest(Iterable<T> iterable) {
        record SequenceLength(T value, int length) {}
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(sequence -> new SequenceLength(sequence, sequence.length()))
                .max(Comparator.comparingInt(SequenceLength::length))
                .map(SequenceLength::value);
    }

    record GenericValue<T>(T value, int length) {}

}
