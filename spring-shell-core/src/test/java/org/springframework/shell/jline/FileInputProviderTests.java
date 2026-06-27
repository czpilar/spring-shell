/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.shell.jline;

import org.jline.reader.EOFError;
import org.jline.reader.impl.DefaultParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.shell.Input;

import java.io.Reader;
import java.io.StringReader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileInputProviderTests {

	private final ExtendedDefaultParser springParser = new ExtendedDefaultParser();
	private final DefaultParser jlineParser = new DefaultParser();
	private FileInputProvider fileInputProvider;

	static Stream<Arguments> regularLinesUnclosedQuotes() {
		return Stream.of(
				Arguments.of("Regular line with unclosed 'quote"),
				Arguments.of("Regular line with unclosed \"quote")
		);
	}

	static Stream<Arguments> commentsUnclosedQuotes() {
		return Stream.of(
				Arguments.of("//Commented line with unclosed 'quote"),
				Arguments.of("//Commented line with unclosed \"quote")
		);
	}

	@ParameterizedTest
	@MethodSource("regularLinesUnclosedQuotes")
	void shouldThrowOnUnclosedQuoteDefaultParser(String line) {
		jlineParser.setEofOnUnclosedQuote(true);
		Reader reader = new StringReader(line);
		fileInputProvider = new FileInputProvider(reader, jlineParser);
		Exception exception = assertThrows(EOFError.class, () -> {
			fileInputProvider.readInput();
		});
		String expectedExceptionMessage = "Missing closing quote";
		String actualExceptionMessage = exception.getMessage();
		assertTrue(actualExceptionMessage.contains(expectedExceptionMessage));
	}

	@ParameterizedTest
	@MethodSource("regularLinesUnclosedQuotes")
	void shouldThrowOnUnclosedQuoteExtendedParser(String line) {
		springParser.setEofOnUnclosedQuote(true);
		Reader reader = new StringReader(line);
		fileInputProvider = new FileInputProvider(reader, springParser);
		Exception exception = assertThrows(EOFError.class, () -> {
			fileInputProvider.readInput();
		});
		String expectedExceptionMessage = "Missing closing quote";
		String actualExceptionMessage = exception.getMessage();
		assertTrue(actualExceptionMessage.contains(expectedExceptionMessage));
	}

	@ParameterizedTest
	@MethodSource("commentsUnclosedQuotes")
	void shouldNotThrowOnUnclosedQuoteDefaultParser(String line) {
		jlineParser.setEofOnUnclosedQuote(true);
		Reader reader = new StringReader(line);
		fileInputProvider = new FileInputProvider(reader, jlineParser);
		assertDoesNotThrow(() -> {
			fileInputProvider.readInput();
		});
	}

	@ParameterizedTest
	@MethodSource("commentsUnclosedQuotes")
	void shouldNotThrowOnUnclosedQuoteExtendedParser(String line) {
		springParser.setEofOnUnclosedQuote(true);
		Reader reader = new StringReader(line);
		fileInputProvider = new FileInputProvider(reader, springParser);
		assertDoesNotThrow(() -> {
			fileInputProvider.readInput();
		});
	}

	@Test
	void shouldSkipEmptyAndCommentedLines() {
		String inputContent = """
				echo Hello World
				// This is a comment
				echo Line 1\\
				Line 2

				echo Line 3""";
		Reader reader = new StringReader(inputContent);
		fileInputProvider = new FileInputProvider(reader, springParser);

		Input first = fileInputProvider.readInput();
		Input second = fileInputProvider.readInput();
		Input third = fileInputProvider.readInput();
		Input fourth = fileInputProvider.readInput();

		assertEquals("echo Hello World", first.rawText());
		assertEquals("echo Line 1 Line 2", second.rawText());
		assertEquals("echo Line 3", third.rawText());
		assertNull(fourth);
	}
}