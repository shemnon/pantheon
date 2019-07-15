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
package tech.pegasys.pantheon.ethereum.trie;

import static tech.pegasys.pantheon.crypto.Hash.keccak256;

import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.bytes.MutableBytesValue;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

class BranchNode<V> implements Node<V> {
  public static final byte RADIX = CompactEncoding.LEAF_TERMINATOR;

  @SuppressWarnings("rawtypes")
  private static final Node NULL_NODE = NullNode.instance();

  private final ArrayList<Node<V>> children;
  private final Optional<V> value;
  private final NodeFactory<V> nodeFactory;
  private final Function<V, BytesValue> valueSerializer;
  private WeakReference<BytesValue> rlp;
  private SoftReference<Bytes32> hash;
  private boolean dirty = false;

  BranchNode(
      final ArrayList<Node<V>> children,
      final Optional<V> value,
      final NodeFactory<V> nodeFactory,
      final Function<V, BytesValue> valueSerializer) {
    assert (children.size() == RADIX);
    this.children = children;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
  }

  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final BytesValue path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final NodeVisitor<V> visitor) {
    visitor.visit(this);
  }

  @Override
  public BytesValue getPath() {
    return BytesValue.EMPTY;
  }

  @Override
  public Optional<V> getValue() {
    return value;
  }

  @Override
  public List<Node<V>> getChildren() {
    return Collections.unmodifiableList(children);
  }

  public Node<V> child(final byte index) {
    return children.get(index);
  }

  @Override
  public BytesValue getRlp() {
    if (rlp != null) {
      final BytesValue encoded = rlp.get();
      if (encoded != null) {
        return encoded;
      }
    }
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    for (int i = 0; i < RADIX; ++i) {
      out.writeRLPUnsafe(children.get(i).getRlpRef());
    }
    if (value.isPresent()) {
      out.writeBytesValue(valueSerializer.apply(value.get()));
    } else {
      out.writeNull();
    }
    out.endList();
    final BytesValue encoded = out.encoded();
    rlp = new WeakReference<>(encoded);
    return encoded;
  }

  @Override
  public BytesValue getRlpRef() {
    if (isReferencedByHash()) {
      return RLP.encodeOne(getHash());
    } else {
      return getRlp();
    }
  }

  @Override
  public Bytes32 getHash() {
    if (hash != null) {
      final Bytes32 hashed = hash.get();
      if (hashed != null) {
        return hashed;
      }
    }
    final Bytes32 hashed = keccak256(getRlp());
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  @Override
  public Node<V> replacePath(final BytesValue newPath) {
    return nodeFactory.createExtension(newPath, this);
  }

  public Node<V> replaceChild(final byte index, final Node<V> updatedChild) {
    final ArrayList<Node<V>> newChildren = new ArrayList<>(children);
    newChildren.set(index, updatedChild);

    if (updatedChild == NULL_NODE) {
      if (value.isPresent() && !hasChildren()) {
        return nodeFactory.createLeaf(BytesValue.of(index), value.get());
      } else if (!value.isPresent()) {
        final Optional<Node<V>> flattened = maybeFlatten(newChildren);
        if (flattened.isPresent()) {
          return flattened.get();
        }
      }
    }

    return nodeFactory.createBranch(newChildren, value);
  }

  public Node<V> replaceValue(final V value) {
    return nodeFactory.createBranch(children, Optional.of(value));
  }

  public Node<V> removeValue() {
    return maybeFlatten(children).orElse(nodeFactory.createBranch(children, Optional.empty()));
  }

  private boolean hasChildren() {
    for (final Node<V> child : children) {
      if (child != NULL_NODE) {
        return true;
      }
    }
    return false;
  }

  private static <V> Optional<Node<V>> maybeFlatten(final ArrayList<Node<V>> children) {
    final int onlyChildIndex = findOnlyChild(children);
    if (onlyChildIndex >= 0) {
      // replace the path of the only child and return it
      final Node<V> onlyChild = children.get(onlyChildIndex);
      final BytesValue onlyChildPath = onlyChild.getPath();
      final MutableBytesValue completePath = MutableBytesValue.create(1 + onlyChildPath.size());
      completePath.set(0, (byte) onlyChildIndex);
      onlyChildPath.copyTo(completePath, 1);
      return Optional.of(onlyChild.replacePath(completePath));
    }
    return Optional.empty();
  }

  private static <V> int findOnlyChild(final ArrayList<Node<V>> children) {
    int onlyChildIndex = -1;
    assert (children.size() == RADIX);
    for (int i = 0; i < RADIX; ++i) {
      if (children.get(i) != NULL_NODE) {
        if (onlyChildIndex >= 0) {
          return -1;
        }
        onlyChildIndex = i;
      }
    }
    return onlyChildIndex;
  }

  @Override
  public String print() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Branch:");
    builder.append("\n\tRef: ").append(getRlpRef());
    for (int i = 0; i < RADIX; i++) {
      final Node<V> child = child((byte) i);
      if (!Objects.equals(child, NullNode.instance())) {
        final String branchLabel = "[" + Integer.toHexString(i) + "] ";
        final String childRep = child.print().replaceAll("\n\t", "\n\t\t");
        builder.append("\n\t").append(branchLabel).append(childRep);
      }
    }
    builder.append("\n\tValue: ").append(getValue().map(Object::toString).orElse("empty"));
    return builder.toString();
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void markDirty() {
    dirty = true;
  }
}
