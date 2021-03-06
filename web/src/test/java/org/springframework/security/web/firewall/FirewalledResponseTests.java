/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.web.firewall;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Luke Taylor
 * @author Eddú Meléndez
 * @author Gabriel Lavoie
 */
public class FirewalledResponseTests {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private HttpServletResponse response;
	private FirewalledResponse fwResponse;

	@Before
	public void setup() {
		response = mock(HttpServletResponse.class);
		fwResponse = new FirewalledResponse(response);
	}

	@Test
	public void sendRedirectWhenValidThenNoException() throws Exception {
		fwResponse.sendRedirect("/theURL");

		verify(response).sendRedirect("/theURL");
	}

	@Test
	public void sendRedirectWhenNullThenDelegateInvoked() throws Exception {
		fwResponse.sendRedirect(null);

		verify(response).sendRedirect(null);
	}

	@Test
	public void sendRedirectWhenHasCrlfThenThrowsException() throws Exception {
		expectCrlfValidationException();

		fwResponse.sendRedirect("/theURL\r\nsomething");
	}

	@Test
	public void addHeaderWhenValidThenDelegateInvoked() throws Exception {
		fwResponse.addHeader("foo", "bar");

		verify(response).addHeader("foo", "bar");
	}

	@Test
	public void addHeaderWhenNullValueThenDelegateInvoked() throws Exception {
		fwResponse.addHeader("foo", null);

		verify(response).addHeader("foo", null);
	}

	@Test
	public void addHeaderWhenHeaderValueHasCrlfThenException() {
		expectCrlfValidationException();

		fwResponse.addHeader("foo", "abc\r\nContent-Length:100");
	}

	@Test
	public void addHeaderWhenHeaderNameHasCrlfThenException() {
		expectCrlfValidationException();

		fwResponse.addHeader("abc\r\nContent-Length:100", "bar");
	}

	@Test
	public void addCookieWhenValidThenDelegateInvoked() {
		Cookie cookie = new Cookie("foo", "bar");
		cookie.setPath("/foobar");
		cookie.setDomain("foobar");
		cookie.setComment("foobar");

		fwResponse.addCookie(cookie);

		verify(response).addCookie(cookie);
	}
	@Test
	public void addCookieWhenNullThenDelegateInvoked() {
		fwResponse.addCookie(null);

		verify(response).addCookie(null);
	}

	@Test
	public void addCookieWhenCookieNameContainsCrlfThenException() {
		// Constructor validates the name
		Cookie cookie = new Cookie("valid-since-constructor-validates", "bar") {
			@Override
			public String getName() {
				return "foo\r\nbar";
			}

		};
		expectCrlfValidationException();

		fwResponse.addCookie(cookie);
	}

	@Test
	public void addCookieWhenCookieValueContainsCrlfThenException() {
		Cookie cookie = new Cookie("foo", "foo\r\nbar");
		expectCrlfValidationException();

		fwResponse.addCookie(cookie);
	}

	@Test
	public void addCookieWhenCookiePathContainsCrlfThenException() {
		Cookie cookie = new Cookie("foo", "bar");
		cookie.setPath("/foo\r\nbar");
		expectCrlfValidationException();

		fwResponse.addCookie(cookie);
	}

	@Test
	public void addCookieWhenCookieDomainContainsCrlfThenException() {
		Cookie cookie = new Cookie("foo", "bar");
		cookie.setDomain("foo\r\nbar");
		expectCrlfValidationException();

		fwResponse.addCookie(cookie);
	}

	@Test
	public void addCookieWhenCookieCommentContainsCrlfThenException() {
		Cookie cookie = new Cookie("foo", "bar");
		cookie.setComment("foo\r\nbar");
		expectCrlfValidationException();

		fwResponse.addCookie(cookie);
	}

	@Test
	public void rejectAnyLineEndingInNameAndValue() {
		validateLineEnding("foo", "foo\rbar");
		validateLineEnding("foo", "foo\r\nbar");
		validateLineEnding("foo", "foo\nbar");

		validateLineEnding("foo\rbar", "bar");
		validateLineEnding("foo\r\nbar", "bar");
		validateLineEnding("foo\nbar", "bar");
	}

	private void expectCrlfValidationException() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Invalid characters (CR/LF)");
	}

	private void validateLineEnding(String name, String value) {
		try {
			fwResponse.validateCrlf(name, value);
			fail("IllegalArgumentException should have thrown");
		}
		catch (IllegalArgumentException expected) {
		}
	}
}
