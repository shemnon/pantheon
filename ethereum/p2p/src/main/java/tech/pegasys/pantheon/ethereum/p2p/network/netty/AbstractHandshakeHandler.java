/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.p2p.network.netty;

import tech.pegasys.pantheon.ethereum.p2p.api.PeerConnection;
import tech.pegasys.pantheon.ethereum.p2p.rlpx.framing.Framer;
import tech.pegasys.pantheon.ethereum.p2p.rlpx.handshake.Handshaker;
import tech.pegasys.pantheon.ethereum.p2p.rlpx.handshake.ecies.ECIESHandshaker;
import tech.pegasys.pantheon.ethereum.p2p.wire.PeerInfo;
import tech.pegasys.pantheon.ethereum.p2p.wire.SubProtocol;
import tech.pegasys.pantheon.ethereum.p2p.wire.messages.DisconnectMessage;
import tech.pegasys.pantheon.ethereum.p2p.wire.messages.DisconnectMessage.DisconnectReason;
import tech.pegasys.pantheon.ethereum.p2p.wire.messages.HelloMessage;
import tech.pegasys.pantheon.ethereum.p2p.wire.messages.WireMessageCodes;
import tech.pegasys.pantheon.metrics.Counter;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractHandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

  private static final Logger LOG = LogManager.getLogger();

  protected final Handshaker handshaker = new ECIESHandshaker();

  private final PeerInfo ourInfo;

  private final Callbacks callbacks;
  private final PeerConnectionRegistry peerConnectionRegistry;

  private final CompletableFuture<PeerConnection> connectionFuture;
  private final List<SubProtocol> subProtocols;

  private final LabelledMetric<Counter> outboundMessagesCounter;

  AbstractHandshakeHandler(
      final List<SubProtocol> subProtocols,
      final PeerInfo ourInfo,
      final CompletableFuture<PeerConnection> connectionFuture,
      final Callbacks callbacks,
      final PeerConnectionRegistry peerConnectionRegistry,
      final LabelledMetric<Counter> outboundMessagesCounter) {
    this.subProtocols = subProtocols;
    this.ourInfo = ourInfo;
    this.connectionFuture = connectionFuture;
    this.callbacks = callbacks;
    this.peerConnectionRegistry = peerConnectionRegistry;
    this.outboundMessagesCounter = outboundMessagesCounter;
  }

  /**
   * Generates the next message in the handshake sequence.
   *
   * @param msg Incoming Message
   * @return Optional of the next Handshake message that needs to be returned to the peer
   */
  protected abstract Optional<ByteBuf> nextHandshakeMessage(ByteBuf msg);

  @Override
  protected final void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
    final Optional<ByteBuf> nextMsg = nextHandshakeMessage(msg);
    if (nextMsg.isPresent()) {
      ctx.writeAndFlush(nextMsg.get());
    } else if (handshaker.getStatus() != Handshaker.HandshakeStatus.SUCCESS) {
      LOG.debug("waiting for more bytes");
    } else {

      final BytesValue nodeId = handshaker.partyPubKey().getEncodedBytes();
      if (peerConnectionRegistry.isAlreadyConnected(nodeId)) {
        LOG.debug("Rejecting connection from already connected client {}", nodeId);
        ctx.writeAndFlush(
                new OutboundMessage(
                    null, DisconnectMessage.create(DisconnectReason.ALREADY_CONNECTED)))
            .addListener(
                ff -> {
                  ctx.close();
                  connectionFuture.completeExceptionally(
                      new IllegalStateException("Client already connected"));
                });
        return;
      }

      LOG.debug("Sending framed hello");

      // Exchange keys done
      final Framer framer = new Framer(handshaker.secrets());

      final ByteToMessageDecoder deFramer =
          new DeFramer(
              framer, subProtocols, ourInfo, callbacks, connectionFuture, outboundMessagesCounter);

      ctx.channel()
          .pipeline()
          .addFirst(new ValidateFirstOutboundMessage(framer))
          .replace(this, "DeFramer", deFramer);

      ctx.writeAndFlush(new OutboundMessage(null, HelloMessage.create(ourInfo)))
          .addListener(
              ff -> {
                if (ff.isSuccess()) {
                  LOG.debug("Successfully wrote hello message");
                }
              });
      msg.retain();
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable throwable) {
    LOG.debug("Handshake error:", throwable);
    connectionFuture.completeExceptionally(throwable);
    ctx.close();
  }

  /** Ensures that wire hello message is the first message written. */
  private static class ValidateFirstOutboundMessage extends MessageToByteEncoder<OutboundMessage> {
    private final Framer framer;

    private ValidateFirstOutboundMessage(final Framer framer) {
      this.framer = framer;
    }

    @Override
    protected void encode(
        final ChannelHandlerContext context,
        final OutboundMessage outboundMessage,
        final ByteBuf out) {
      if (outboundMessage.getCapability() != null
          || outboundMessage.getData().getCode() != WireMessageCodes.HELLO) {
        throw new IllegalStateException("First wire message sent wasn't a HELLO.");
      }
      framer.frame(outboundMessage.getData(), out);
      context.pipeline().remove(this);
    }
  }
}
