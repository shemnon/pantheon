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
package tech.pegasys.pantheon.ethereum.eth.manager;

import tech.pegasys.pantheon.ethereum.p2p.api.MessageData;
import tech.pegasys.pantheon.ethereum.p2p.api.PeerConnection;
import tech.pegasys.pantheon.ethereum.p2p.peers.DefaultPeer;
import tech.pegasys.pantheon.ethereum.p2p.peers.Peer;
import tech.pegasys.pantheon.ethereum.p2p.wire.Capability;
import tech.pegasys.pantheon.ethereum.p2p.wire.PeerInfo;
import tech.pegasys.pantheon.ethereum.p2p.wire.messages.DisconnectMessage.DisconnectReason;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.enode.EnodeURL;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Set;

public class MockPeerConnection implements PeerConnection {

  private static final PeerSendHandler NOOP_ON_SEND = (cap, msg, conn) -> {};
  private final PeerSendHandler onSend;
  private final Set<Capability> caps;
  private volatile boolean disconnected = false;
  private final BytesValue nodeId;
  private final Peer peer;
  private final PeerInfo peerInfo;

  public MockPeerConnection(final Set<Capability> caps, final PeerSendHandler onSend) {
    this.caps = caps;
    this.onSend = onSend;
    this.nodeId = Peer.randomId();
    this.peer =
        DefaultPeer.fromEnodeURL(
            EnodeURL.builder().ipAddress("127.0.0.1").nodeId(nodeId).listeningPort(30303).build());
    this.peerInfo = new PeerInfo(5, "Mock", new ArrayList<>(caps), 30303, nodeId);
  }

  public MockPeerConnection(final Set<Capability> caps) {
    this(caps, NOOP_ON_SEND);
  }

  @Override
  public void send(final Capability capability, final MessageData message) throws PeerNotConnected {
    if (disconnected) {
      throw new PeerNotConnected("MockPeerConnection disconnected");
    }
    onSend.exec(capability, message, this);
  }

  @Override
  public Set<Capability> getAgreedCapabilities() {
    return caps;
  }

  @Override
  public Peer getPeer() {
    return peer;
  }

  @Override
  public PeerInfo getPeerInfo() {
    return peerInfo;
  }

  @Override
  public void terminateConnection(final DisconnectReason reason, final boolean peerInitiated) {
    disconnect(reason);
  }

  @Override
  public void disconnect(final DisconnectReason reason) {
    disconnected = true;
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDisconnected() {
    return disconnected;
  }

  @FunctionalInterface
  public interface PeerSendHandler {
    void exec(Capability cap, MessageData msg, PeerConnection connection);
  }
}
