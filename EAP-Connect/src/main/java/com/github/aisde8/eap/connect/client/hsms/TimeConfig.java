package com.github.aisde8.eap.connect.client.hsms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeConfig {

    //Linktest Timer 用于周期性地发送 Linktest 消息以确认网络连接是否仍然有效。如果 T1 超时后未收到 Linktest 响应，则认为连接断开。
    @Builder.Default
    private int T1 = 10;

    //Reply Timeout 用于等待发送的 SECS 消息的回复 (例如，对于S1F13 等 W-bit 为 1 的消息)。如果 T2 超时，认为消息未送达或丢失。用于等待发送的 SECS 消息的回复 (例如，对于 $\text{S1F13}$ 等 $\text{W}$-bit 为 1 的消息)。如果 T2 超时，认为消息未送达或丢失。
    @Builder.Default
    private int T2 = 5;

    //Control Transaction Timeout  用于等待控制消息的确认 (例如 Select Request 消息的 Select Response 消息)。如果 T3 超时，认为连接失败。
    @Builder.Default
    private int T3 = 15;

    //Connection Timeout 在 HSMS-SS 中，网络连接由操作系统和网络堆栈管理 。如果 T4 超时，认为连接失败。
    @Builder.Default
    private int T4 = 30;

    //Connect Modulo (连接模数)定义了在尝试建立连接时，重复发送 Select Request 消息的间隔时间。
    @Builder.Default
    private int T5 = 30;

    //Control Transaction Retries 定义了在等待控制消息确认时，最大允许的重试次数。如果超过 T6 次重试仍未收到确认，认为连接失败。
    @Builder.Default
    private int T6 = 30;

    //Not Selected Timeout 连接建立后，等待 Select Request 或 Select Response 消息的最长等待时间 用于确保在连接建立后能迅速进入 $\text{Selected}$ 状态。
    @Builder.Default
    private int T7 = 30;
}
