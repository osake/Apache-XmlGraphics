/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: DataURIResolver.java 1345683 2012-06-03 14:50:33Z gadams $ */

package org.apache.xmlgraphics.util.uri;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import lombok.extern.slf4j.Slf4j;

import org.apache.xmlgraphics.util.io.Base64DecodeStream;

/**
 * Resolves data URLs (described in RFC 2397) returning its data as a
 * StreamSource.
 *
 * @see javax.xml.transform.URIResolver
 * @see <a href="http://www.ietf.org/rfc/rfc2397">RFC 2397</a>
 */
@Slf4j
public class DataURIResolver implements URIResolver {

	/**
	 * {@inheritDoc}
	 */
	public Source resolve(final String href, final String base)
			throws TransformerException {
		if (href.startsWith("data:")) {
			return parseDataURI(href);
		} else {
			return null;
		}
	}

	/**
	 * Parses inline data URIs as generated by MS Word's XML export and FO
	 * stylesheet.
	 *
	 * @see <a href="http://www.ietf.org/rfc/rfc2397">RFC 2397</a>
	 */
	private Source parseDataURI(final String href) {
		final int commaPos = href.indexOf(',');
		// header is of the form data:[<mediatype>][;base64]
		final String header = href.substring(0, commaPos);
		final String data = href.substring(commaPos + 1);
		if (header.endsWith(";base64")) {
			final byte[] bytes = data.getBytes();
			final ByteArrayInputStream encodedStream = new ByteArrayInputStream(
					bytes);
			final Base64DecodeStream decodedStream = new Base64DecodeStream(
					encodedStream);
			return new StreamSource(decodedStream);
		} else {
			String encoding = "UTF-8";
			final int charsetpos = header.indexOf(";charset=");
			if (charsetpos > 0) {
				encoding = header.substring(charsetpos + 9);
			}
			try {
				final String unescapedString = URLDecoder
						.decode(data, encoding);
				return new StreamSource(new java.io.StringReader(
						unescapedString));
			} catch (final IllegalArgumentException e) {
				log.error(e.getMessage(), e);
			} catch (final UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

}