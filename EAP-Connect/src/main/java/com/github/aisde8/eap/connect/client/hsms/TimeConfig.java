package com.github.aisde8.eap.connect.client.hsms;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeConfig {

    @Builder.Default
    private Integer T3 = 15;

    @Builder.Default
    private Integer T1 = 10;

    @Builder.Default
    private Integer T2 = 5;


}
