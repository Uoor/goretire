package com.aliren.houserent.subscribe;

import com.aliren.houserent.match.client.MatchClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionParserTest {

    @Mock
    private MatchClient matchClient;

    private SubscriptionParser parser;

    @BeforeEach
    void setUp() {
        parser = new SubscriptionParser(matchClient);
    }

    @Test
    void parse_llmOutput_returnsStructuredJson() {
        when(matchClient.complete(anyString()))
                .thenReturn("{\"region\":\"杭州西溪\",\"maxRent\":6000,\"houseType\":\"两居\",\"petOk\":1}");

        String json = parser.parse("西溪附近 6000 以内两居，能养猫");
        assertThat(json).isNotNull();
        assertThat(json).contains("\"region\":\"杭州西溪\"").contains("\"maxRent\":6000").contains("\"petOk\":1");
    }

    @Test
    void parse_llmUnavailable_fallsBackToKeywordExtraction() {
        when(matchClient.complete(anyString())).thenReturn(null);

        String json = parser.parse("杭州西溪 4000-6000 两居，能养猫");
        assertThat(json).isNotNull();
        assertThat(json).contains("\"region\":\"杭州西溪\"")
                .contains("\"minRent\":4000").contains("\"maxRent\":6000")
                .contains("\"houseType\":\"两居\"").contains("\"petOk\":1");
    }

    @Test
    void parse_llmGarbage_fallsBack() {
        when(matchClient.complete(anyString())).thenReturn("抱歉，我无法理解");

        String json = parser.parse("北京望京 7500 以内");
        assertThat(json).isNotNull();
        assertThat(json).contains("\"region\":\"北京望京\"").contains("\"maxRent\":7500");
    }

    @Test
    void parse_unparseable_returnsNull() {
        when(matchClient.complete(anyString())).thenReturn(null);
        assertThat(parser.parse("随便看看")).isNull();
        assertThat(parser.parse(null)).isNull();
        assertThat(parser.parse("  ")).isNull();
    }
}
