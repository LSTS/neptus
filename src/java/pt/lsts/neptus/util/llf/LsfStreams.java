/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2022/03/01
 */
package pt.lsts.neptus.util.llf;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;

public class LsfStreams {

    static final int MIN_STREAM_SIZE = 500;

    /**
     * Creates a Stream for a specific message type
     * @param <T> The message type
     * @param index The index to stream
     * @param type The message type encoded as the representing class 
     * @return a Stream for the specific message type
     */
    public static <T extends IMCMessage> Stream<T> stream(LsfIndex index, Class<T> type) {
        return StreamSupport.stream(spliterator(index, type), true);
    }

    /**
     * Creates a Stream for a list of message types
     * @param <T> The message types
     * @param index The index to stream
     * @param type The message types encoded as the representing class (varargs)
     * @return a Stream for the given message types
     */
    public static Stream<IMCMessage> stream(LsfIndex index, Class<? extends IMCMessage>... types) {
        return StreamSupport.stream(multiSpliterator(index, types), true);
    }

    /**
     * Create a spliterator for a number of message types
     * @param index The index from where to iterate messages
     * @param types The message types to be iterated
     * @return Spliterator for the given message types
     */
    public static Spliterator<IMCMessage> multiSpliterator(LsfIndex index, Class<? extends IMCMessage>... types) {
        int[] mgTypes = new int[types.length];
        for (int i = 0; i < types.length; i++)
            mgTypes[i] = index.getDefinitions().getMessageId(types[i].getSimpleName());
        return new LsfMultiSpliterator(index, mgTypes);
    }

    /**
     * Create a spliterator for a specific message type
     * @param <T> The message type
     * @param index The index to iterate
     * @param type The message type encoded  as the representing class
     * @return Spliterator for the specific message type
     */
    public static <T extends IMCMessage> Spliterator<T> spliterator(LsfIndex index, Class<T> type) {
        return new LsfSpliterator<T>(index, type);
    }

    private static class LsfSpliterator<M extends IMCMessage> implements Spliterator<M> {
        private int startIndex, endIndex;
        private LsfIndex index;
        private Class<M> mgType;
        int type;

        public LsfSpliterator(LsfIndex index, Class<M> type) {
            this(index, 0, index.getNumberOfMessages() - 1, type);
        }

        private LsfSpliterator(LsfIndex index, int startIndex, int endIndex, Class<M> mgType) {
            this.index = index;
            this.mgType = mgType;
            this.type = index.getDefinitions().getMessageId(mgType.getSimpleName());
            this.mgType = mgType;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public Spliterator<M> trySplit() {
            if (endIndex - startIndex < MIN_STREAM_SIZE)
                return null;

            int splitIndex = startIndex + (endIndex - startIndex) / 2;
            int oldEndIndex = endIndex;
            this.endIndex = splitIndex - 1;
            return new LsfSpliterator<M>(index, splitIndex, oldEndIndex, mgType);
        }

        @Override
        public long estimateSize() {
            return endIndex - startIndex;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }

        @Override
        public boolean tryAdvance(Consumer<? super M> action) {
            while (startIndex < endIndex) {
                if (index.typeOf(startIndex) == type) {
                    try {
                        action.accept(index.getMessage(startIndex++, mgType));
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    return true;
                }
                startIndex++;
            }
            return false;
        }
    }

    private static class LsfMultiSpliterator implements Spliterator<IMCMessage> {
        private int[] types;
        private int startIndex, endIndex;
        private LsfIndex index;

        public LsfMultiSpliterator(LsfIndex index, int... types) {
            this(index, 0, index.getNumberOfMessages() - 1, types);
        }

        public LsfMultiSpliterator(LsfIndex index, int startIndex, int endIndex, int... types) {
            this.index = index;
            this.types = types;
            Arrays.sort(this.types);
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public boolean tryAdvance(Consumer<? super IMCMessage> action) {
            while (startIndex < endIndex) {
                int pos = Arrays.binarySearch(types, index.typeOf(startIndex));
                if (pos > 0 && pos < types.length) {
                    action.accept(index.getMessage(startIndex++));
                    return true;
                }
                startIndex++;
            }
            return false;
        }

        @Override
        public Spliterator<IMCMessage> trySplit() {
            if (endIndex - startIndex < MIN_STREAM_SIZE)
                return null;

            int splitIndex = startIndex + (endIndex - startIndex) / 2;
            int oldEndIndex = endIndex;
            this.endIndex = splitIndex - 1;
            return new LsfMultiSpliterator(index, splitIndex, oldEndIndex, types);
        }

        @Override
        public long estimateSize() {
            return endIndex - startIndex;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }
    }
}
