/**
 * Copyright 2014 Peter Foldes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.soliton.protobuf;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link io.soliton.protobuf.Client} collection interface for managing
 * multiple clients.
 *
 * @author Peter Foldes (peter.foldes@gmail.com)
 */
public class ClientPool implements Client, List<Client> {

  private final CopyOnWriteArrayList<Client> clientList =
      new CopyOnWriteArrayList<>();
  private final SelectionPolicy policy;

  public ClientPool(SelectionPolicy policy) {
    this.policy = policy;
  }

  @Override
  public <O extends Message> ListenableFuture<O> encodeMethodCall(
      ClientMethod<O> method, Message input) {
    return nextClient().encodeMethodCall(method, input);
  }

  /**
   * Returns the next client from the pool based on the
   * {@link io.soliton.protobuf.SelectionPolicy}.
   *
   * <p>
   *   Can return null if there's no client available.
   * </p>
   */
  @Nullable
  public Client nextClient() {
    return this.policy.select(clientList);
  }

  // Below are the proxy methods for the List interface

  @Override
  public int size() {
    return this.clientList.size();
  }

  @Override
  public boolean isEmpty() {
    return this.clientList.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return this.clientList.contains(o);
  }

  @Override
  public Iterator<Client> iterator() {
    return this.clientList.iterator();
  }

  @Override
  public Object[] toArray() {
    return this.clientList.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return this.clientList.toArray(a);
  }

  @Override
  public boolean add(Client client) {
    return this.clientList.add(client);
  }

  @Override
  public boolean remove(Object o) {
    return this.clientList.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return this.clientList.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends Client> c) {
    return this.clientList.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends Client> c) {
    return this.clientList.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return this.clientList.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return this.clientList.retainAll(c);
  }

  @Override
  public void clear() {
    this.clientList.clear();
  }

  @Override
  public Client get(int index) {
    return this.clientList.get(index);
  }

  @Override
  public Client set(int index, Client element) {
    return this.clientList.set(index, element);
  }

  @Override
  public void add(int index, Client element) {
    this.clientList.add(index, element);
  }

  @Override
  public Client remove(int index) {
    return this.clientList.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return this.clientList.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return this.clientList.lastIndexOf(o);
  }

  @Override
  public ListIterator<Client> listIterator() {
    return this.clientList.listIterator();
  }

  @Override
  public ListIterator<Client> listIterator(int index) {
    return this.clientList.listIterator(index);
  }

  @Override
  public List<Client> subList(int fromIndex, int toIndex) {
    return this.clientList.subList(fromIndex, toIndex);
  }

}
