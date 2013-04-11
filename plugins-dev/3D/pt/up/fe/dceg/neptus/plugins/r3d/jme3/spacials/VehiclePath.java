/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Margarida Faria
 * Jul 9, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3.spacials;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3i;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.VehicleInfoAtPointDTO;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.JmeComponent;
import pt.up.fe.dceg.neptus.plugins.r3d.jme3.WorldInformation;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;

/**
 * Creates 3D representations that shows the path of the vehicle:
 * <p>- a line based on data gathered for height map
 * <p>- a strip directly based on data gathered from log 
 * 
 * @author Margarida Faria
 * 
 */
public class VehiclePath extends Element3D {
    private static final double WINDOW_RELEVANT_MOVEMENT = 0.1;

    private boolean showPath, showPathOptm;
    private final Geometry path;
    private Geometry pathOptmUp;
    private final ArrayList<VehicleInfoAtPointDTO> pathPoints;


    /**
     * Sets up variables for toggle.
     * <p>Creates the line that marks the vehicle path and adds it to root node.
     * <p>Creates a strip that also marks the vehicle path.
     * 
     * @param bathyData object with information about the path and conversion of coordinates
     * @param fatherNode the node where the geometries will be added
     * @param assetManager the asset manager of the game
     * @param worldInfo all the info for scaling and converting points
     */
    public VehiclePath(BathymetryLogInfo bathyData, Node fatherNode, AssetManager assetManager, WorldInformation worldInfo) {
        super(assetManager, fatherNode, worldInfo);
        showPath = true;
        showPathOptm = false;
        // Line
        path = buildCurve(getPathPointsInJME(bathyData), Spline.SplineType.Linear, "vehiclePath");
        if (JmeComponent.TEST_CURVE) {
            // Strip
            pathPoints = bathyData.getVehicleInfo();
            createStripNewDataStruct();
        }
        else {
            pathPoints = null;
        }
    }

    /**
     * Removes path line if visible, shows it otherwise.
     */
    public void togglePath() {
        if (showPath) {
            fatherNode.detachChild(path);
        }
        else {
            fatherNode.attachChild(path);
        }
        showPath = !showPath;
    }

    /**
     * Removes path strip if visible, shows it otherwise.
     */
    public void togglePathOptimized() {
        if (showPathOptm) {
            // fatherNode.detachChild(pathOptmDown);
            fatherNode.detachChild(pathOptmUp);
        }
        else {
            // fatherNode.attachChild(pathOptmDown);
            fatherNode.attachChild(pathOptmUp);
        }
        showPathOptm = !showPathOptm;
    }


    /**
     * Build vehicle path as a line with data gathered for the terrain height map.
     * 
     * @param bathyData information on the path of the vehicle from log
     * @return a vector that with the points of the path organized as a vector of 3 coordinates vector
     */
    private Vector<Vector3f> getPathPointsInJME(BathymetryLogInfo bathyData) {
        Vector<Vector3f> points = new Vector<Vector3f>();
        Vector3f wayPoint;

        Iterator<Double> northIterator = bathyData.getNorthVec().iterator();
        Iterator<Double> eastIterator = bathyData.getEastVec().iterator();
        Iterator<Double> depthIterator = bathyData.getDepthVec().iterator();

        Double northOffset, eastOffset;
        while (northIterator.hasNext() && eastIterator.hasNext()
                && depthIterator.hasNext()) {
            northOffset = northIterator.next();
            eastOffset = eastIterator.next();
            // North East
            // North >> - y Image px >> z jME coordinates
            // East >> x Image px >> x jME coordinates
            // [N][E] >> [y Img][x Img] >> [z jME][x jME]
            float offset[] = { northOffset.floatValue(), eastOffset.floatValue() };
            // NeptusLog.pub().info("<###>[VehiclePath] offsets(" + offset[0] + ", " + offset[1] + ")");
            offset = worldInfo.convertNED2jME_heightMapScale(offset); // [y Image px][x Image px]
            // NeptusLog.pub().info("<###>[VehiclePath] JME x z (" + offset[1] + ", " + -offset[0] + ")");
            // Depth
            // Transforms depth into height then scales to what height is maped in the height map
            float heightConverted = worldInfo.convertDepthMeter2Px_heightMapScale(depthIterator.next().floatValue());
             heightConverted = (heightConverted == 0 ? 0 : heightConverted);
            // wayPoint has jME coordinates
            wayPoint = new Vector3f(offset[1], heightConverted, -offset[0]);
            points.add(wayPoint);
        }
        return points;
    }

    private boolean createStripNewDataStruct() {
        int skiped = 0;

        int pointNumber = pathPoints.size();
        int maxSegments = 40;
        int countSegments = 0;
        if (pointNumber < 2) {
            NeptusLog.pub().info(
                    I18n.textf("INPUT ERROR! Trying to visualize a path with %pointNumber positions!", pointNumber));
            return false;
        }

        ArrayList<Integer> middleResUp_500 = new ArrayList<Integer>();
        ArrayList<Integer> middleResDown_500 = new ArrayList<Integer>();
        ArrayList<Integer> lowResUp_1000 = new ArrayList<Integer>();
        ArrayList<Integer> lowResDown_1000 = new ArrayList<Integer>();

        Iterator<VehicleInfoAtPointDTO> pathIt = pathPoints.iterator();
        VehicleInfoAtPointDTO pointA, pointB;
        pointA = pathIt.next();
        pointA.setPositionXYZ(worldInfo.convertLatLonDepth2XYZ(pointA.getLatLonDepth()));
        calcAndSetWingPoints(pointA);

        // setup vertices arrays
        // final int triNumber = (pointNumber - 1) * 2;
        final int triNumber = (maxSegments - 1) * 2;
        int up[] = new int[triNumber * 3];
        int down[] = new int[triNumber * 3];
        final int vertNumber = maxSegments * 2;
        Vector3f[] finalVertices = new Vector3f[vertNumber];
        finalVertices[0] = pointA.getWingLeft();
        finalVertices[1] = pointA.getWingRight();
        NeptusLog.pub().info("<###>First central point:" + pointA.getPositionXYZ().toString());

        int localIndexTimes2;
        ArrayList<int[]> vertexIndexUpDown;
        int locationIndex = 0;
        float distance;
        while (countSegments < maxSegments && pathIt.hasNext()) {
            countSegments++;
            localIndexTimes2 = locationIndex * 2;
            // Get location and calculate wing points for it
            pointB = pathIt.next();
            pointB.setPositionXYZ(worldInfo.convertLatLonDepth2XYZ(pointB.getLatLonDepth()));

            distance = pointA.getPositionXYZ().distance(pointB.getPositionXYZ());
            if (distance < (WINDOW_RELEVANT_MOVEMENT * worldInfo.SCALE_METERS2JME_DIRECT_FROM_LOG)) {
                // to small to build more vertexs
                System.out.print(" -> " + distance);
                skiped++;
                continue;
            }
            System.out.println();

            System.out.println(locationIndex + " central point:" + pointB.getPositionXYZ().toString());
            calcAndSetWingPoints(pointB);
            // Set those points in the final vertex array
            finalVertices[localIndexTimes2 + 2] = pointB.getWingLeft();
            finalVertices[localIndexTimes2 + 3] = pointB.getWingRight();

            // Set the current array with the wing points of this and the past point
            vertexIndexUpDown = calcVertexOrder(pointA, pointB);

            // 2 triangle are generated at each step
            // 6 numbers indicate their order
            for (int i = 0; i < 6; i++) {
                up[locationIndex * 6 + i] = vertexIndexUpDown.get(0)[i] + (localIndexTimes2);
                down[locationIndex * 6 + i] = vertexIndexUpDown.get(1)[i] + (localIndexTimes2);
            }
            if (middleResUp_500.size() > 0 && Math.abs(pointB.getRoll()) > 0.1) {
                addQuadIfNecessary(middleResUp_500, middleResDown_500, pointB, finalVertices, localIndexTimes2, 50,
                        vertexIndexUpDown);
                // addQuadIfNecessary(lowResUp_1000, lowResDown_1000, pointB, finalVertices, localIndexTimes2, 200,
                // vertexIndexUpDown);
            }
            else {
                for (int i = 0; i < 6; i++) {
                    middleResUp_500.add(vertexIndexUpDown.get(0)[i] + (localIndexTimes2));
                    middleResDown_500.add(vertexIndexUpDown.get(1)[i] + (localIndexTimes2));
                    lowResUp_1000.add(vertexIndexUpDown.get(0)[i] + (localIndexTimes2));
                    lowResDown_1000.add(vertexIndexUpDown.get(1)[i] + (localIndexTimes2));
                }
            }

            System.out.println();
            System.out.println(finalVertices[up[localIndexTimes2 + 0]] + " - "
                    + finalVertices[up[localIndexTimes2 + 1]] + " - " + finalVertices[up[localIndexTimes2 + 2]]
                    + " is visible? "
                    + isVisible(Arrays.copyOfRange(up, localIndexTimes2 + 0, localIndexTimes2 + 3), finalVertices));
            System.out.println(finalVertices[up[localIndexTimes2 + 0]] + " - "
                    + finalVertices[up[localIndexTimes2 + 1]] + " - " + finalVertices[up[localIndexTimes2 + 2]]
                    + " is visible? "
                    + isVisible(Arrays.copyOfRange(up, localIndexTimes2 + 3, localIndexTimes2 + 6), finalVertices));

            pointA = pointB;
            locationIndex++;
        }

        NeptusLog.pub().info("<###>Of " + maxSegments + " position, skiped " + skiped + ". " + (locationIndex + 1)
                + " positions considered");
        printVector3fArray(finalVertices);
        printIntArray(up);

        // Create LODs
        int lodLevels = 2;
        VertexBuffer[] lodsUp = new VertexBuffer[lodLevels];
        VertexBuffer[] lodsDown = new VertexBuffer[lodLevels];
        int[][] lodDataUp = new int[lodLevels][];
        int[][] lodDataDown = new int[lodLevels][];
        lodDataUp[0] = new int[middleResUp_500.size()];
        lodDataUp[1] = new int[lowResUp_1000.size()];
        lodDataDown[0] = new int[middleResDown_500.size()];
        lodDataDown[1] = new int[lowResDown_1000.size()];
        int i;
        for (i = 0; i < lodDataDown[1].length; i++) {
            lodDataUp[0][i] = middleResUp_500.get(i);
            lodDataDown[0][i] = middleResDown_500.get(i);
            lodDataUp[1][i] = lowResUp_1000.get(i);
            lodDataDown[1][i] = lowResDown_1000.get(i);

        }
        for (; i < lodDataUp[0].length; i++) {
            lodDataUp[0][i] = middleResUp_500.get(i);
            lodDataDown[0][i] = middleResDown_500.get(i);
        }

        for (i = 0; i < lodLevels; i++) {
            lodsUp[i] = new VertexBuffer(Type.Index);
            lodsUp[i].setupData(Usage.Dynamic, 1, Format.UnsignedInt, BufferUtils.createIntBuffer(lodDataUp[i]));
            lodsDown[i] = new VertexBuffer(Type.Index);
            lodsDown[i].setupData(Usage.Dynamic, 1, Format.UnsignedInt, BufferUtils.createIntBuffer(lodDataUp[i]));
        }
        // with lod
        // pathOptmUp = createMesh(finalVertices, up, "upside", ColorRGBA.Yellow, lodsUp);
        // pathOptmDown = createMesh(finalVertices, down, "downside", ColorRGBA.Orange, lodsDown);

        // no Lod
        // pathOptmDown = createMesh(finalVertices, down, "downside", ColorRGBA.Orange, null);
        pathOptmUp = createMesh(finalVertices, up, "upside", ColorRGBA.Yellow, null);

        // fatherNode.detachChild(pathOptmDown);
        fatherNode.detachChild(pathOptmUp);
        return true;
    }

    private void printVector3fArray(Vector3f[] array) {
        System.out.print("( ");
        for (int i = 0; i < array.length; i++) {
            Vector3f j = array[i];
            if (j == null)
                break;
            System.out.print(j.toString() + "  ");
        }
        NeptusLog.pub().info("<###>)");
    }

    private void printIntArray(int[] array) {
        System.out.print("( ");
        for (int i = 0; i < array.length; i++) {
            int j = array[i];
            System.out.print(j + "  ");
        }
        NeptusLog.pub().info("<###>)");
    }

    private boolean isVisible(int[] order, Vector3f[] finalVertices) {
        // for a triangle p1, p2, p3, if the vector U = p2 - p1 and the vector V = p3 - p1
        Vector3f u = finalVertices[order[1]].subtractLocal(finalVertices[order[0]]);
        Vector3f v = finalVertices[order[2]].subtractLocal(finalVertices[order[0]]);
        // then the normal N = U X V
        // To find back facing polygons the dot product of the surface normal of each polygon is taken with a vector
        // from the center of projection to any point on the polygon.
        // a surface normal can be calculated as the vector cross product of two (non-parallel) edges of the polygon
        Vector3f surfaceNormal = u.crossLocal(v);
        //
        // The dot product is then used to determine what direction the polygon is facing:
        // greater than 0 : back facing
        // equal to 0 : polygon viewed on edge
        // less than 0 : front facing
        float camLvl = worldInfo.getTerrainLvl() + ((worldInfo.getGeoidtLvl() - worldInfo.getTerrainLvl()) / 2);
        float dot = new Vector3f(0, camLvl, 0).dot(surfaceNormal);
        if (dot > 0) {
            return false;
        }
        else {
            return true;
        }
    }

    private void addQuadIfNecessary(ArrayList<Integer> diffResVertexOrderUp, ArrayList<Integer> diffResVertexOrderDown,
            VehicleInfoAtPointDTO pointB, Vector3f[] verticesCoord, int localIndexTimes2, int minRelevantDistance,
            ArrayList<int[]> mainVertexIndexUpDown) {
        int numIndexes;
        Point3i lastTriIndexesUp;
        numIndexes = diffResVertexOrderUp.size();
        lastTriIndexesUp = new Point3i();
        lastTriIndexesUp.x = diffResVertexOrderUp.get(numIndexes - 3);
        lastTriIndexesUp.y = diffResVertexOrderUp.get(numIndexes - 2);
        lastTriIndexesUp.z = diffResVertexOrderUp.get(numIndexes - 1);
        Vector3f lastTriVertex = verticesCoord[lastTriIndexesUp.y];
//        int minRelevantDistance = 50;
        if (lastTriVertex.distance(pointB.getPositionXYZ()) > minRelevantDistance) {
            // sharp turn
            // middle res
            Point3i currTriIndexesUp;
            currTriIndexesUp = new Point3i();
            currTriIndexesUp.x = mainVertexIndexUpDown.get(0)[0] + (localIndexTimes2);
            currTriIndexesUp.y = mainVertexIndexUpDown.get(0)[1] + (localIndexTimes2);
            currTriIndexesUp.z = mainVertexIndexUpDown.get(0)[2] + (localIndexTimes2);
            // if it's not adjacent a connection Quad must be made
            if (isAdjacent(lastTriIndexesUp, currTriIndexesUp) == false) {
                ArrayList<int[]> connectionQuadVertexOrder = calcConectionQuadVertexOrder(diffResVertexOrderUp,
                        verticesCoord, pointB, localIndexTimes2);
                for (int i = 0; i < 6; i++) {
                    diffResVertexOrderUp.add(connectionQuadVertexOrder.get(0)[i]);
                    diffResVertexOrderDown.add(connectionQuadVertexOrder.get(1)[i]);
                }
            }
        }
    }

    /**
     * Calculate the order of the vertex of two triangles (quad) to connect the previous quad to the next.
     * <p>
     * Designed for meshes of strips of less resolution LODs.
     * 
     * @param indexList the ordered indexed of triangles so far
     * @param finalVertexes all the vertexes associated with the mesh
     * @param pointB the point that needs to be connected to the last existing one
     * @param locationIndexOffset the offset to match the wings points of pointB to their position in the finalVertexes
     * @return an array with the ordered indexes of the vertexes of the connection quad, already with the offsets to
     *         match finalVertexes
     */
    private ArrayList<int[]> calcConectionQuadVertexOrder(ArrayList<Integer> indexList, Vector3f[] finalVertexes,
            VehicleInfoAtPointDTO pointB, int locationIndexOffset) {
        // Find out the wings of the last point
        // This uses the fact that the array with coordinates is always built the same way (for each point first put
        // the wing point on the same direction of the normal of (unit x + yaw + roll) and then the point in the
        // opposite direction)
        int iOfLast = indexList.size() - 1;
        Vector3f leftWing = new Vector3f();
        Vector3f rightWing = new Vector3f();
        leftWing = finalVertexes[indexList.get(iOfLast - 1)]; // 4
        rightWing = finalVertexes[indexList.get(iOfLast)]; // 5
        Vector3f wings[] = { leftWing, rightWing };

        // now we calculate vertex order between those wings and the wings of the current point
        ArrayList<int[]> vertexOrder = calcVertexOrder(wings, pointB);
        // Finally go through the arrays to add the correspondent offsets:
        // - for the old point, the index of their points in finalVertexes
        // - for the new point, the offset of point index * 2 (passed by parameter)
        int iMinLast = Math.min(indexList.get(iOfLast), indexList.get(iOfLast - 1));
        int length = 6;
        for (int j = 0; j < length; j++) {
            // if it's from last triangle add smaller offset
            if (vertexOrder.get(0)[j] < 2) {
                vertexOrder.get(0)[j] += iMinLast;
            }
            // if it's from current location add current location offset
            else {
                vertexOrder.get(0)[j] += locationIndexOffset;
            }
            // do the same for the other side
            if (vertexOrder.get(1)[j] < 2) {
                vertexOrder.get(1)[j] += iMinLast;
            }
            else {
                vertexOrder.get(1)[j] += locationIndexOffset;
            }
        }
        return vertexOrder;
    }

    private ArrayList<int[]> calcVertexOrder(VehicleInfoAtPointDTO pointA, VehicleInfoAtPointDTO pointB) {
        Vector3f wingsA[] = { pointA.getWingLeft(), pointA.getWingRight() };
        return calcVertexOrder(wingsA, pointB);
    }

    private ArrayList<int[]> calcVertexOrder(Vector3f wingsA[], VehicleInfoAtPointDTO pointB) {
        Vector3f[] currVertices = new Vector3f[4];
        // Set the current array with the wing points of this and the past point
        currVertices[0] = wingsA[0];
        currVertices[1] = wingsA[1];
        currVertices[2] = pointB.getWingLeft();
        currVertices[3] = pointB.getWingRight();
        // Calculate how to put them to create one mesh clockwise and another counter clockwise
        return orderVertexIndexes(currVertices);
    }

    /**
     * The vertex with higher index is a wing point of the more advanced point. Since the triangle that has as vertexes
     * two wings points of the last point also has one from the wing points of the point before, in adjacent quads the
     * highest index of the last triangle has to be lower than the lowest index of the first triangle of the next quad.
     * 
     * left wing A ------ left wing B 0 | \ | 2 | \ | point A \ point B| \ | 1 | \ | 3 right wing A -----right wing B
     * 
     * @param last triangle of last quad
     * @param current first triangle of current quad
     * @return
     */
    private boolean isAdjacent(Point3i last, Point3i current) {
        int iMaxLast = Math.max(last.x, last.y);
        iMaxLast = Math.max(iMaxLast, last.z);
        int iMinCurrent = Math.min(current.x, current.y);
        iMinCurrent = Math.min(iMinCurrent, current.z);
        return (iMinCurrent <= iMaxLast);
    }

    private void calcAndSetWingPoints(VehicleInfoAtPointDTO centralPoint) {
        Vector3f location = centralPoint.getPositionXYZ();
        float distance = 2f;
        // Create vector that is the reference if there is no yaw
        // => no yaw = UNIT_X
        Vector3f sidePos = new Vector3f(1, 0, 0);
        Vector3f sideNeg = new Vector3f(-1, 0, 0);

        // Add length = distance to wing points
        sidePos.multLocal(distance);
        sideNeg.multLocal(distance);

        Quaternion yawRoll = new Quaternion();
        yawRoll.fromAngles(0, centralPoint.getYaw(), centralPoint.getRoll());
        // yawRoll.fromAngles(0, FastMath.QUARTER_PI, FastMath.QUARTER_PI);
        sidePos = yawRoll.mult(sidePos);

        yawRoll = new Quaternion();
        yawRoll.fromAngles(0, -centralPoint.getYaw(), -centralPoint.getRoll());
        // yawRoll.fromAngles(0, -FastMath.QUARTER_PI, 0);
        sideNeg = yawRoll.mult(sideNeg);

        centralPoint.setWings(sidePos.add(location), sideNeg.add(location));

        // Create vector that is the reference if there is no yaw
        // yaw rotates around depth/-y, since the wing points are on the east direction and east=xjME
        // => no yaw = UNIT_X
        // Vector3f side = new Vector3f(1, 0, 0);
        // Add length = distance to wing points
        // side = side.multLocal(distance);
        // Rotate according to yaw
        // Quaternion yawRoll = new Quaternion();
        // TODO add yaw
        // yawRoll.fromAngleAxis(centralPoint.getYaw(), Vector3f.UNIT_Y);
        // yawRoll.multLocal(side);

        // TODO add roll
        // Now add the roll angle
        // Roll rotates around north/-z
        // yawRoll.fromAngleAxis(centralPoint.getRoll(), Vector3f.UNIT_Z.negate());
        // side = yawRoll.multLocal(side);

        // Apply point
        // TODO reflect second wing on normal of surface
        // R = V - 2*(V.N) * N;
        // Vector3f wing__sidex = side.add(location);
        // Vector3f wing__negate = new Vector3f(x, y, z)
        // centralPoint.setWings(wing__1_0_0, side.negate().add(location));

    }

    private enum WingPoint {
        Left, Right;
    }
    
    /**
     * Find out which point of the wings of the second point in quad should be used as common point between the two
     * adjacent triangles.
     * 
     * @param vertices in the order: left wing of first point, right wing of first point, left wing of second point,
     *            right wing of second point.
     * @return the wing point of second point that should be used (left[0] or right[1])
     */
    @SuppressWarnings("unused")
    private WingPoint whichWingPointOf2ndointWillBeCommon(Vector3f[] vertices) {
        // Calculate distances between wings to know what points to use
        ArrayList<Float> distances = new ArrayList<Float>();
        distances.add(0, vertices[0].distance(vertices[2])); // distAC
        distances.add(1, vertices[1].distance(vertices[2])); // distBC
        distances.add(2, vertices[0].distance(vertices[3])); // distAD
        distances.add(3, vertices[1].distance(vertices[3])); // distBD
        Float distance, minDistance;
        int iMinDistance = -1;
        int count = 0;
        minDistance = Float.MAX_VALUE;
        // We want the minimum distance
        for (Iterator<Float> it = distances.iterator(); it.hasNext();) {
            distance = it.next();
            if (distance < minDistance) {
                minDistance = distance;
                iMinDistance = count;
            }
            count++;
        }
        switch (iMinDistance){
            case 0:
            case 1:
                return WingPoint.Left;
            case 2:
            case 3:
                return WingPoint.Right;
            default: 
                NeptusLog.pub().error("There is an algorithm error in VehiclePath.whichWingPointOf2ndointWillBeCommon().\n" +
                		"Please report the following as a bug in https://whale.fe.up.pt/redmine/projects/3dviz/issues/new:\n" +
                		"Vertices: "+vertices[0].toString()+", "+vertices[1].toString()+", "+vertices[2].toString()+", "+vertices[3].toString()+" \n" +
                		"Distances: "+distances.get(0)+", "+distances.get(1)+", "+distances.get(2)+", "+distances.get(3)+"\n" +
        				"Minimum distance: ["+iMinDistance+"]="+distances.get(iMinDistance));
                return null;
        }
    }

    /**
     * Set the order of each vertex.
     * <p> 
     * This has to guarantee that each triangle represents common points in reverse order while maintaining counter clockwise order among them and choosing the shortest distance for diagonal of the quad:
     * <p> - the shortest distance is guaranteed by choosing which point of the 1st wing will be in the 2nd triangle and vice-versa according to the shortest distance between them
     * <p> - the reverse order is guaranteed by setting the 1st wing common point 1t in the 1st triangle and the 2nd wing common point in the 2nd triangle
     * <p> - the counter clock wise order is guaranteed by always selecting for the downface the normal to the triangle that has negative y
     * 
     * @param vertices
     * @return
     */
    // private ArrayList<int[]> orderVertexIndexes(Vector3f[] vertices) {
    // // Calculate which points are closest
    // WingPoint commonWingP2ndP = whichWingPointOf2ndointWillBeCommon(vertices);
    // // Based on the distances set the common points of the wings for each triangle
    // int common2ndWingPDown, common1stWingPDown, opposite2ndWingPDown, opposite1stWingPDown;
    // opposite1stWingPDown = opposite2ndWingPDown = common1stWingPDown = common2ndWingPDown = -1;
    // switch (commonWingP2ndP) {
    // case Left: // distAC or distBC are the shortest
    // // For the first triangle use the first point in wing B
    // common2ndWingPDown = 2;
    // opposite2ndWingPDown = 3;
    // // For the 2nd triangle use the second point in wing A
    // common1stWingPDown = 1;
    // opposite1stWingPDown = 0;
    // break;
    // case Right: // distAD or distBD are the shortest
    // // For the first triangle use the second point in wing B
    // common2ndWingPDown = 3;
    // opposite2ndWingPDown = 2;
    // // For the 2nd triangle use the second point in wing A
    // common1stWingPDown = 0;
    // opposite1stWingPDown = 1;
    // break;
    // }
    // int[] indexesDown = new int[6];
    // int[] indexesUp = new int[6];
    // // First triangle
    // boolean isCCW_Down = isCounterClock(vertices[common1stWingPDown], vertices[opposite1stWingPDown],
    // vertices[common2ndWingPDown]);
    // indexesDown[0] = common1stWingPDown;
    // indexesUp[0] = common1stWingPDown;
    // if (isCCW_Down) {
    // // Down
    // indexesDown[1] = opposite1stWingPDown;
    // indexesDown[2] = common2ndWingPDown;
    // // Up
    // indexesUp[1] = common2ndWingPDown;
    // indexesUp[2] = opposite1stWingPDown;
    // }
    // else {
    // // Down
    // indexesDown[1] = common2ndWingPDown;
    // indexesDown[2] = opposite1stWingPDown;
    // // Up
    // indexesUp[1] = opposite1stWingPDown;
    // indexesUp[2] = common2ndWingPDown;
    // }
    // // Second triangle
    // isCCW_Down = isCounterClock(vertices[common2ndWingPDown], vertices[common1stWingPDown],
    // vertices[opposite2ndWingPDown]);
    // indexesDown[3] = common2ndWingPDown;
    // indexesUp[3] = common2ndWingPDown;
    // if (isCCW_Down) {
    // // Down
    // indexesDown[4] = common1stWingPDown;
    // indexesDown[5] = opposite2ndWingPDown;
    // // Up
    // indexesUp[4] = opposite2ndWingPDown;
    // indexesUp[5] = common1stWingPDown;
    // }
    // else {
    // // Down
    // indexesDown[4] = opposite2ndWingPDown;
    // indexesDown[5] = common1stWingPDown;
    // // Up
    // indexesUp[4] = common1stWingPDown;
    // indexesUp[5] = opposite2ndWingPDown;
    // }
    // ArrayList<int[]> vertexIndexUpDown = new ArrayList<int[]>(2);
    // vertexIndexUpDown.add(0, indexesUp);
    // vertexIndexUpDown.add(1, indexesDown);
    // return vertexIndexUpDown;
    // }

    /**
     * From 3 points that define a plane calculate if, in the current order, their normal points to y
     * 
     * @param p1
     * @param p2
     * @param p3
     * @return true if it points to y
     *         <p>
     *         if it points to -y
     */
    // private boolean isCounterClock(Vector3f p1, Vector3f p2, Vector3f p3) {
    // Vector3f normal = FastMath.computeNormal(p1, p2, p3);
    // // the normal vector for the downwards face must always have negative y
    // if (normal.y <= 0f) {
    // return true;
    // }
    // else {
    // return false;
    // }
    // }

    // private ArrayList<int[]> calcVertexOrder(Vector3f wingsA[], Vector3f wingsB[]) {
    // Vector3f[] currVertices = new Vector3f[4];
    // // Set the current array with the wing points of this and the past point
    // currVertices[0] = wingsA[0];
    // currVertices[1] = wingsA[1];
    // currVertices[2] = wingsB[0];
    // currVertices[3] = wingsB[1];
    // // Calculate how to put them to create one mesh clockwise and another counter clockwise
    // return orderVertexIndexes(currVertices);
    // }

    /**
     * Set the order of each vertex.
     * <p> 
     * This has to guarantee that each triangle represents common points in reverse order while maintaining counter clockwise order among them and choosing the shortest distance for diagonal of the quad:
     * <p> - the shortest distance is guaranteed by choosing which point of the 1st wing will be in the 2nd triangle and vice-versa according to the shortest distance between them
     * <p> - the reverse order is guaranteed by setting the 1st wing common point 1t in the 1st triangle and the 2nd wing common point in the 2nd triangle
     * <p> - the counter clock wise order is guaranteed by always selecting for the downface the normal to the triangle that has negative y
     * 
     * @param vertices
     * @return
     */
    private ArrayList<int[]> orderVertexIndexes(Vector3f[] vertices) {
        boolean shortest12 = is12ShortestThan03(vertices);
        int common2ndWingPDown, common1stWingPDown, opposite2ndWingPDown, opposite1stWingPDown;
        opposite1stWingPDown = opposite2ndWingPDown = common1stWingPDown = common2ndWingPDown = -1;
        if (shortest12) {
            common2ndWingPDown = 2;
            opposite2ndWingPDown = 3;
            common1stWingPDown = 1;
            opposite1stWingPDown = 0;
        }
        else { // shortest
            common2ndWingPDown = 3;
            opposite2ndWingPDown = 2;
            common1stWingPDown = 0;
            opposite1stWingPDown = 1;

        }

        int[] indexesDown = new int[6];
        int[] indexesUp = new int[6];
        // First triangle
        boolean isCCW_Down = isCounterClock(vertices[opposite1stWingPDown], vertices[common1stWingPDown],
                vertices[common2ndWingPDown]);
        indexesDown[0] = opposite1stWingPDown;
        indexesUp[0] = opposite1stWingPDown;
        if (isCCW_Down) {
            indexesDown[1] = common1stWingPDown;
            indexesDown[2] = common2ndWingPDown;
            // Second triangle
            indexesDown[3] = common2ndWingPDown;
            indexesDown[4] = common1stWingPDown;
            indexesDown[5] = opposite2ndWingPDown;
            // - Up
            indexesUp[1] = common2ndWingPDown;
            indexesUp[2] = common1stWingPDown;
            // Second triangle
            indexesUp[3] = common1stWingPDown;
            indexesUp[4] = common2ndWingPDown;
            indexesUp[5] = opposite2ndWingPDown;
        }
        else {
            indexesDown[1] = common2ndWingPDown;
            indexesDown[2] = common1stWingPDown;
            // Second triangle
            indexesDown[3] = common1stWingPDown;
            indexesDown[4] = common2ndWingPDown;
            indexesDown[5] = opposite2ndWingPDown;
            // - Up
            indexesUp[1] = common1stWingPDown;
            indexesUp[2] = common2ndWingPDown;
            // Second triangle
            indexesUp[3] = common2ndWingPDown;
            indexesUp[4] = common1stWingPDown;
            indexesUp[5] = opposite2ndWingPDown;
        }

        ArrayList<int[]> vertexIndexUpDown = new ArrayList<int[]>(2);
        vertexIndexUpDown.add(0, indexesUp);
        vertexIndexUpDown.add(1, indexesDown);
        return vertexIndexUpDown;
    }

    /**
     * From 3 points that define a plane calculate if, in the current order, their normal points to y
     * 
     * @param p1
     * @param p2
     * @param p3
     * @return true if it points to y
     *         <p>
     *         if it points to -y
     */
    private boolean isCounterClock(Vector3f p1, Vector3f p2, Vector3f p3) {
        Vector3f normal = FastMath.computeNormal(p1, p2, p3);
        // the normal vector for the downwards face must always have negative y
        if (normal.y <= 0f) {
            return true;
        }
        else {
            return false;
        }
    }

    private boolean is12ShortestThan03(Vector3f[] vertices) {
        float distances[] = new float[2];
        distances[0] = vertices[1].distance(vertices[2]); // distBC
        distances[1] = vertices[0].distance(vertices[3]); // distAD
        if (distances[0] > distances[1]) {
            return false;
        }
        else {
            return true;
        }
    }
}