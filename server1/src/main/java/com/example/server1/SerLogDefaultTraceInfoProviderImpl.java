package com.example.server1;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 
 * @author vinhnh
 *
 */
public class SerLogDefaultTraceInfoProviderImpl implements SerLogTraceInfoProvider {

	@Autowired
	private Tracer tracer;

	@Override
	public Map<String, String> getTraceInfo() {
		Map<String, String> baggageWithTraceInfo = new HashMap<>();
		tracer.getAllBaggage().forEach((key, value) -> {
			if (StringUtils.isNotBlank(value)) {
				baggageWithTraceInfo.put(key, value);
			}
		});

		Optional<Span> currentSpan = Optional.ofNullable(tracer.currentSpan());
		currentSpan.map(Span::context)
				.ifPresent(currentSpanContext -> {
					baggageWithTraceInfo.put("x-trace-id", currentSpanContext.traceId());
					baggageWithTraceInfo.put("x-span-id", currentSpanContext.spanId());
					if (StringUtils.isNotBlank(currentSpanContext.parentId())) {
						baggageWithTraceInfo.put("x-parent-span-id", currentSpanContext.parentId());
					}
				});
		return baggageWithTraceInfo;
	}

}
