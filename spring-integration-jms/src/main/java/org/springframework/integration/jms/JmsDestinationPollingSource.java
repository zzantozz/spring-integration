/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageSource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsMessageDrivenEndpoint} that uses Spring's MessageListener container
 * support is a better option.
 * 
 * @author Mark Fisher
 */
public class JmsDestinationPollingSource extends AbstractJmsTemplateBasedAdapter implements MessageSource<Object> {

	private volatile boolean extractPayload = true;

	private volatile String messageSelector;


	public JmsDestinationPollingSource(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public JmsDestinationPollingSource(ConnectionFactory connectionFactory, Destination destination) {
		super(connectionFactory, destination);
	}

	public JmsDestinationPollingSource(ConnectionFactory connectionFactory, String destinationName) {
		super(connectionFactory, destinationName);
	}


	/**
	 * Specify a JMS Message Selector expression to use when receiving Messages.
	 */
	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	/**
	 * Specify a MessageConverter to use when mapping from JMS Mesages to
	 * Spring Integration Messages. If it is not itself an implementation
	 * of {@link HeaderMappingMessageConverter}, it will be wrapped.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.getJmsTemplate().setMessageConverter(messageConverter);
	}

	/**
	 * Specify whether the payload should be extracted from each received JMS
	 * Message to be used as the Spring Integration Message payload.
	 * 
	 * <p>The default value is <code>true</code>. To force creation of Spring
	 * Integration Messages whose payload is the actual JMS Message, set this
	 * to <code>false</code>.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@SuppressWarnings("unchecked")
	public Message<Object> receive() {
		Object receivedObject = this.getJmsTemplate().receiveSelectedAndConvert(this.messageSelector);
		if (receivedObject == null) {
			return null;
		}
		if (receivedObject instanceof Message) {
			return (Message) receivedObject;
		}
		return new GenericMessage<Object>(receivedObject);
	}

	@Override
	protected void configureMessageConverter(JmsTemplate jmsTemplate, JmsHeaderMapper headerMapper) {
		MessageConverter converter = jmsTemplate.getMessageConverter();
		if (converter == null || !(converter instanceof HeaderMappingMessageConverter)) {
			HeaderMappingMessageConverter hmmc = new HeaderMappingMessageConverter(converter, headerMapper);
			hmmc.setExtractJmsMessageBody(this.extractPayload);
			jmsTemplate.setMessageConverter(hmmc);
		}
	}

}