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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jun 13, 2014
 */
package pt.lsts.neptus.mra.api;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.BigByteBuffer;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

/**
 * @author zp
 *
 */
public class CorrectedPosition {

    protected static final String FILENAME_POS = "mra/positions.cache";
    protected static final String FILENAME_IDX = "mra/positions.index";

    /*
     * [Header]: 13 bytes 0 - 'I' 1 - 'D' 2 - 'X' 3 - 'P' 4 - '1' 5 - start timestamp (long)
     *
     * [Entry]: 16 bytes 0 - time (int) 4 - pos (long) 12 - ser_size (int)
     */
    protected int IDX_HEADER_SIZE = 13;
    protected int IDX_ENTRY_SIZE = 16;
    protected int IDX_OFFSET_OF_TIME = 0, IDX_OFFSET_OF_POS = 4, IDX_OFFSET_OF_SIZE = 12;


    //private ArrayList<SystemPositionAndAttitude> positions = new ArrayList<>();

    protected boolean loaded = false;
    protected File indexFile;
    protected File posFile;

    protected BigByteBuffer indexBuffer;
    protected BigByteBuffer posBuffer;

    protected long startTime;
    protected long curTime;
    protected long endTime;
    protected long numberOfIndexes;

    protected RandomAccessFile posInputStream;
	protected FileInputStream indexInputStream;
	protected FileChannel posChannel;
	protected FileChannel indexChannel;

    protected Consumer<String> progressListener;

    public CorrectedPosition(IMraLogGroup source) {
        this(source, null);
    }

    public CorrectedPosition(IMraLogGroup source, Consumer<String> progressListener) {
        this.progressListener = progressListener;
        new File(source.getDir(), "mra").mkdirs();
        load(source);
        this.progressListener = null;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public long getSize() {
        return numberOfIndexes;
    }

    public Iterator<SystemPositionAndAttitude> iterator() {
        return new CurrectPositionIterator(this);
    }

    public void cleanup() {
        loaded = false;
        if (posChannel != null) {
            try {
                posChannel.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (indexChannel != null) {
            try {
                indexChannel.close();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        posBuffer.getBuffer().clear();
        posBuffer = null;
        indexBuffer.getBuffer().clear();
        indexBuffer = null;

        numberOfIndexes = -1;
    }

    private void warnProgress(String message) {
        if (progressListener != null) {
            progressListener.accept(message);
        }
    }

    private long getIndexFilePosFromIndex(long index) {
        return IDX_HEADER_SIZE + index * IDX_ENTRY_SIZE;
    }

    private long getIndexFileTimePosFromIndex(long index) {
        return getIndexFilePosFromIndex(index) + IDX_OFFSET_OF_TIME;
    }

    private boolean load(IMraLogGroup source) {
        synchronized (source) {
            if (!loadIndex(source)) {
                // Create cache and index
                createIndex(source);
                return loadIndex(source);
            }

            return true;
        }
    }

    private boolean loadIndex(IMraLogGroup source) {
        synchronized (source) {
            File cacheFx = new File(source.getDir(), FILENAME_POS);
            File indexFx = new File(source.getDir(), FILENAME_IDX);

            try {
                if (cacheFx.canRead() && indexFx.canRead()) {
                    posInputStream = new RandomAccessFile(cacheFx, "r");
                    posChannel = posInputStream.getChannel();
                    posBuffer = new BigByteBuffer(posChannel, cacheFx.length());
                    posBuffer.order(ByteOrder.BIG_ENDIAN); // Needs to be big endian!

                    indexInputStream = new FileInputStream(indexFx);
                    long indexSize = indexFx.length();
                    indexChannel = indexInputStream.getChannel();
                    indexBuffer = new BigByteBuffer(indexChannel, indexFx.length());
                    indexBuffer.order(ByteOrder.BIG_ENDIAN); // Needs to be big endian!

                    if (indexBuffer.getBuffer().get() != 'I' || indexBuffer.getBuffer().get() != 'D'
                            || indexBuffer.getBuffer().get() != 'X' || indexBuffer.getBuffer().get() != 'P'
                            || indexBuffer.getBuffer().get() != '1') {
                        throw new Exception(
                                "The index file is not valid. Please regenerate the index.");
                    }
                    curTime = startTime = indexBuffer.getBuffer().getLong();

                    numberOfIndexes = Math.max(0, indexSize - IDX_HEADER_SIZE) / IDX_ENTRY_SIZE;
                    endTime = getEndTime();

                    String msg = "Read " + numberOfIndexes + " positions from cache file.";
                    NeptusLog.pub().info(msg);
                    warnProgress(msg);

                    loaded = true;
                    return true;
                }
            }
            catch (Exception e) {
                String msg = "Positions cache not found. Creating new one.";
                NeptusLog.pub().warn(msg);
                warnProgress(msg);
            }

            indexFile = indexFx;
            posFile = cacheFx;

            loaded = false;
            return false;
        }
    }

    private class IndexesHolder {
        long indexFilePos = 0;
        long posFilePos = 0;
        long posWCounter = 0;
    }

    protected void createIndex(IMraLogGroup source) {
        String msg = I18n.text("Creating positions cache");
        NeptusLog.pub().info(msg);
        warnProgress(msg);

        new File(source.getDir(), "mra").mkdirs();
        new File(FILENAME_POS).delete();
        new File(FILENAME_IDX).delete();

        try (DataOutputStream posDos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(posFile.toPath())));
             DataOutputStream indexDos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(indexFile.toPath())))) {

            IndexesHolder indexesHolder = new IndexesHolder();

            LsfIterator<EstimatedState> it = source.getLsfIndex().getIterator(EstimatedState.class);
            long stepTime = 100;
            CorrectedPositionBuilder cpBuilder = new CorrectedPositionBuilder();

            source.getLsfIndex().hasMultipleVehicles();
            Collection<Integer> systemsLst = source.getVehicleSources();
            int sysToUse = systemsLst.iterator().next();
            long prevTime = -1;

            long cnt = 0;
            for (EstimatedState es = it.next(); es != null; es = it.next()) {
                if (es.getSrc() != sysToUse)
                    continue;

                if (++cnt % 10 == 0) {
                    warnProgress("Reading " + cnt + " positions");
                }

                long diffT = es.getTimestampMillis() - prevTime;
                if (diffT < stepTime)
                    continue;
                prevTime = es.getTimestampMillis();

                List<SystemPositionAndAttitude> retPositions = cpBuilder.update(es);
                saveToFiles(posDos, indexDos, indexesHolder, retPositions);
            }

            List<SystemPositionAndAttitude> retPositions = cpBuilder.getRemainingPositions();
            saveToFiles(posDos, indexDos, indexesHolder, retPositions);

            cpBuilder.reset();
            warnProgress("Finished " + cnt + " positions");
        }
        catch (Exception e) {
            String msg1 = "Positions cache not created. Error: " + e.getMessage();
            NeptusLog.pub().error(msg1);
            warnProgress(msg1);
        }
        catch (Error e) {
            String msg1 = "Positions cache not created. Error: " + e.getMessage();
            NeptusLog.pub().error(msg1);
            warnProgress(msg1);
        }
    }

    private void saveToFiles(DataOutputStream posDos, DataOutputStream indexDos, IndexesHolder indexesHolder,
                             List<SystemPositionAndAttitude> retPositions) {
        for (SystemPositionAndAttitude sysPosAtt : retPositions) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ObjectOutputStream ous = new ObjectOutputStream(baos)) {
                ous.writeObject(sysPosAtt);
                baos.flush();
                byte[] ba = baos.toByteArray();
                posDos.write(ba);
                long posWritePos = indexesHolder.posFilePos;
                indexesHolder.posFilePos += ba.length;
                indexesHolder.posWCounter++;

                long time = sysPosAtt.getTime();

                if (indexesHolder.indexFilePos == 0) {
                    indexDos.write(new byte[] { 'I', 'D', 'X', 'P', '1' });
                    indexDos.writeLong(time);
                    startTime = time;
                    indexesHolder.indexFilePos += IDX_HEADER_SIZE;
                }
                indexDos.writeInt((int) (time - startTime)); // delta time
                indexDos.writeLong(posWritePos); // pos in file
                indexDos.writeInt(ba.length); // ser size
                indexesHolder.indexFilePos += IDX_ENTRY_SIZE;

                if (indexesHolder.posWCounter % 10 == 0) {
                    warnProgress("Writing " + indexesHolder.posWCounter + " positions...");
                    posDos.flush();
                    indexDos.flush();
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error saving positions cache to " + FILENAME_POS);
                e.printStackTrace();
            }
        }
    }

    public long getStartTime() {
        return timeOf(0);
    }

    public long getEndTime() {
        long idx = numberOfIndexes - 1;
        return timeOf(idx);
    }

    public long timeOf(long index) {
        if (!isLoaded() || index > numberOfIndexes || !indexBuffer.position(getIndexFileTimePosFromIndex(index))) {
            return -1;
        }

        return startTime + indexBuffer.getBuffer().getInt();
    }



//    @SuppressWarnings("unchecked")
//    private boolean load1(IMraLogGroup source) {
//        synchronized (source) {
//            File index = new File(source.getDir(), FILENAME_IDX);
//            File cache = new File(source.getDir(), FILENAME_POS);
//            try {
//                if (source.getFile("mra/positions.cache").canRead()) {
//                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache));
//                    positions = (ArrayList<SystemPositionAndAttitude>) ois.readObject();
//                    ois.close();
//                    NeptusLog.pub().info("Read " + positions.size() + " positions from cache file.");
//                    return;
//                }
//            }
//            catch (Exception e) {
//                NeptusLog.pub().warn("Positions cache not found. Creating new one.");
//            }
//
//            LsfIterator<EstimatedState> it = source.getLsfIndex().getIterator(EstimatedState.class);
//            long stepTime = 100;
//            CorrectedPositionBuilder cpBuilder = new CorrectedPositionBuilder();
//
//            source.getLsfIndex().hasMultipleVehicles();
//            Collection<Integer> systemsLst = source.getVehicleSources();
//            int sysToUse = systemsLst.iterator().next();
//            long prevTime = -1;
//
//            for (EstimatedState es = it.next(); es != null; es = it.next()) {
//                if (es.getSrc() != sysToUse)
//                    continue;
//
//                long diffT = es.getTimestampMillis() - prevTime;
//                if (diffT < stepTime)
//                    continue;
//                prevTime = es.getTimestampMillis();
//                cpBuilder.update(es);
//            }
//
//            positions = cpBuilder.getPositions();
//
//            try {
//                ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(cache));
//                ous.writeObject(positions);
//                ous.close();
//                NeptusLog.pub().info("Wrote " + positions.size() + " positions to cache file.");
//            }
//            catch (Exception e) {
//                NeptusLog.pub().error("Error saving positions cache to " + cache);
//                e.printStackTrace();
//            }
//        }
//    }

//    public void  CorrectedPosition2(IMraLogGroup source) {
//        synchronized (source) {
//            File cache = new File(source.getDir(), "mra/positions.cache");
//            try {
//                if (source.getFile("mra/positions.cache").canRead()) {
//                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache));
//                    positions = (ArrayList<SystemPositionAndAttitude>) ois.readObject();
//                    ois.close();
//                    NeptusLog.pub().info("Read " + positions.size() + " positions from cache file.");
//                    return;
//                }
//            }
//            catch (Exception e) {
//                NeptusLog.pub().warn("Positions cache not found. Creating new one.");
//            }
//
//            LsfIterator<EstimatedState> it = source.getLsfIndex().getIterator(EstimatedState.class);
//            long stepTime = 100;
//            CorrectedPositionBuilder cpBuilder = new CorrectedPositionBuilder();
//
//            source.getLsfIndex().hasMultipleVehicles();
//            Collection<Integer> systemsLst = source.getVehicleSources();
//            int sysToUse = systemsLst.iterator().next();
//            long prevTime = -1;
//
//            for (EstimatedState es = it.next(); es != null; es = it.next()) {
//                if (es.getSrc() != sysToUse)
//                    continue;
//
//                long diffT = es.getTimestampMillis() - prevTime;
//                if (diffT < stepTime)
//                    continue;
//                prevTime = es.getTimestampMillis();
//                cpBuilder.update(es);
//            }
//
//            positions = cpBuilder.getPositions();
//
//            try {
//                ObjectOutputStream ous = new ObjectOutputStream(new FileOutputStream(cache));
//                ous.writeObject(positions);
//                ous.close();
//                NeptusLog.pub().info("Wrote " + positions.size() + " positions to cache file.");
//            }
//            catch (Exception e) {
//                NeptusLog.pub().error("Error saving positions cache to " + cache);
//                e.printStackTrace();
//            }
//        }
//    }

//    /**
//     * @return the positions
//     */
//    public Collection<SystemPositionAndAttitude> getPositions() {
//        return Collections.unmodifiableCollection(positions);
//    }

//    public SystemPositionAndAttitude getPosition(double timestamp) {
//        if (positions.isEmpty())
//            return null;
//        SystemPositionAndAttitude p = new SystemPositionAndAttitude();
//        p.setTime((long) (timestamp * 1000.0));
//        int pos = Collections.binarySearch(positions, p);
//        if (pos < 0)
//            pos = -pos;
//        if (pos >= positions.size())
//            return positions.get(positions.size() - 1);
//        return positions.get(pos);
//    }

    public SystemPositionAndAttitude getPosition(double timestamp) {
        if (!isLoaded() || numberOfIndexes == 0) {
            return null;
        }

        long targetTime = (long) (timestamp * 1000.0);
        long low = 0, high = numberOfIndexes - 1;
        long index = -1;

        // binarySearch
        while (low <= high) {
            long mid = low  + ((high - low) / 2);
            if (timeOf(mid) < targetTime) {
                low = mid + 1;
            } else if (timeOf(mid) > targetTime) {
                high = mid - 1;
            } else if (timeOf(mid) == targetTime) {
                index = mid;
                break;
            }
        }
        if (index == -1) {
            index =  low + 1;  // key not found
        }

        if (index >= numberOfIndexes) {
            return getSysPos(numberOfIndexes - 1);
        }
        return getSysPos(index);
    }

    protected SystemPositionAndAttitude getSysPos(long index) {
        if (!isLoaded() || index > numberOfIndexes || !indexBuffer.position(getIndexFilePosFromIndex(index))) {
            return null;
        }

        int deltaTime = indexBuffer.getBuffer().getInt();
        long filePos = indexBuffer.getBuffer().getLong();
        int serSize = indexBuffer.getBuffer().getInt();

        if (!posBuffer.position(filePos)) {
            return null;
        }

        ByteBuffer bbuf = ByteBuffer.allocate(serSize);
        posBuffer.getBuffer().get(bbuf.array());

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bbuf.array());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            SystemPositionAndAttitude sysPosition = (SystemPositionAndAttitude) ois.readObject();
            return sysPosition;
        }
        catch (Exception e) {
            String msg = "Not able to get position for index " + index + "!";
            NeptusLog.pub().warn(msg);
            warnProgress(msg);
        }

        return null;
    }
}
