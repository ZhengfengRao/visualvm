/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.charts.xy;

import org.graalvm.visualvm.lib.charts.ItemSelection;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;
import java.util.Locale;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartItem;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.XYItem;
import org.graalvm.visualvm.lib.charts.xy.XYItemSelection;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYChartContext;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemPainter;

/**
 *
 * @author Jiri Sedlacek
 */
public class XYPainter extends SynchronousXYItemPainter {
    
    private final int mode;

    private final Color fillColor2;
    private boolean painting;

    
    // --- Initializer ---------------------------------------------------------
    
    {
        String _mode = System.getProperty("visualvm.charts.defaultMode", "minmax").toLowerCase(Locale.ENGLISH); // NOI18N
        if ("fast".equals(_mode)) { // NOI18N
            mode = 0;
        } else {
            mode = 1;
        }
    }

    // --- Constructor ---------------------------------------------------------

    public static XYPainter absolutePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor1,
                                                       Color fillColor2) {

        return new XYPainter(lineWidth, lineColor, fillColor1, fillColor2,
                                         TYPE_ABSOLUTE, 0);
    }

    public static XYPainter relativePainter(float lineWidth,
                                                       Color lineColor,
                                                       Color fillColor1,
                                                       Color fillColor2,
                                                       int maxOffset) {

        return new XYPainter(lineWidth, lineColor, fillColor1, fillColor2,
                                         TYPE_RELATIVE, maxOffset);
    }


    public XYPainter(float lineWidth, Color lineColor, Color fillColor1,
                     Color fillColor2, int type, int maxValueOffset) {

        super(lineWidth, lineColor, fillColor1, type, maxValueOffset);
        this.fillColor2 = Utils.checkedColor(fillColor2);
        painting = true;
    }


    // --- Public interface ----------------------------------------------------

    public void setPainting(boolean painting) {
        this.painting = painting;
    }

    public boolean isPainting() {
        return painting;
    }


    // --- ItemPainter implementation ------------------------------------------

    public LongRect getSelectionBounds(ItemSelection selection, ChartContext context) {

        XYItemSelection sel = (XYItemSelection)selection;
        XYItem item  = sel.getItem();
        int selectedValueIndex = sel.getValueIndex();

        if (selectedValueIndex == -1 ||
            selectedValueIndex >= item.getValuesCount())
            // This happens on reset - bounds of the selection are unknown, let's clear whole area
            return new LongRect(0, 0, context.getViewportWidth(),
                                context.getViewportHeight());
        else
            return getViewBounds(item, new int[] { sel.getValueIndex() }, context);
    }

    public XYItemSelection getClosestSelection(ChartItem item, int viewX,
                                               int viewY, ChartContext context) {

        if (mode == 1) return getMinMaxClosestSelection(item, viewX, viewY, context);
        else if (mode == 0) return getFastClosestSelection(item, viewX, viewY, context);
        else return null;
    }
    
    private int[][] getPoints(XYItem item, Rectangle dirtyArea,
                              SynchronousXYChartContext context,
                              int type, int maxValueOffset) {
        
        if (mode == 1) return getMinMaxPoints(item, dirtyArea, context, type, maxValueOffset);
        else if (mode == 0) return getFastPoints(item, dirtyArea, context, type, maxValueOffset);
        else return null;
    }

    protected void paint(XYItem item, List<ItemSelection> highlighted,
                       List<ItemSelection> selected, Graphics2D g,
                       Rectangle dirtyArea, SynchronousXYChartContext context) {
        
        if (!isPainting()) return;
        if (item.getValuesCount() < 2) return;
        if (context.getViewWidth() == 0 || context.getViewHeight() == 0) return;

        int[][] points = getPoints(item, dirtyArea, context, type, maxValueOffset);
        if (points == null) return;

        int[] xPoints  = points[0];
        int[] yPoints  = points[1];
        int npoints = points[2][0];
        
        if (fillColor != null) {
            int zeroY = Utils.checkedInt(context.getViewY(context.getDataOffsetY()));
            zeroY = Math.max(Utils.checkedInt(context.getViewportOffsetY()), zeroY);
            zeroY = Math.min(Utils.checkedInt(context.getViewportOffsetY() +
                                                      context.getViewportHeight()), zeroY);

            Polygon polygon = new Polygon();
            polygon.xpoints = xPoints;
            polygon.ypoints = yPoints;
            polygon.npoints = npoints + 2;
            polygon.xpoints[npoints] = xPoints[npoints - 1];
            polygon.ypoints[npoints] = zeroY;
            polygon.xpoints[npoints + 1] = xPoints[0];
            polygon.ypoints[npoints + 1] = zeroY;
            
            if (fillColor2 == null || Utils.forceSpeed()) g.setPaint(fillColor);
            else g.setPaint(new GradientPaint(0, context.getViewportOffsetY(),
                           fillColor, 0, context.getViewportOffsetY() +
                           context.getViewportHeight(), fillColor2));
            g.fill(polygon);
        }

        if (lineColor != null) {
            g.setPaint(lineColor);
            g.setStroke(lineStroke);
            g.drawPolyline(xPoints, yPoints, npoints);
        }

    }
    
    
    private XYItemSelection getFastClosestSelection(ChartItem item, int viewX,
                                                    int viewY, ChartContext context) {

        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;

        int nearestTimestampIndex = contx.getNearestTimestampIndex(viewX, viewY);
        if (nearestTimestampIndex == -1) return null; // item not visible

        SynchronousXYItem xyItem = (SynchronousXYItem)item;
        return new XYItemSelection.Default(xyItem, nearestTimestampIndex,
                                           ItemSelection.DISTANCE_UNKNOWN);
    }
    
    private XYItemSelection getMinMaxClosestSelection(ChartItem item, int viewX,
                                                      int viewY, ChartContext context) {

        SynchronousXYItem xyItem = (SynchronousXYItem)item;
        if (xyItem.getValuesCount() == 0) return null;
        
        SynchronousXYChartContext contx = (SynchronousXYChartContext)context;
        Rectangle bounds = new Rectangle(0, 0, contx.getViewportWidth(), contx.getViewportHeight());
        if (bounds.isEmpty()) return null;
        
        int[][] visibleBounds = contx.getVisibleBounds(bounds);
        if (visibleBounds[0][0] == -1 && visibleBounds[0][1] == -1) return null;
        else if (visibleBounds[1][0] == -1 && visibleBounds[1][1] == -1) return null;
        
        int firstVisible = visibleBounds[0][0];
        if (firstVisible == -1) firstVisible = visibleBounds[0][1];
        
        int lastVisible = visibleBounds[1][0];
        if (lastVisible == -1) lastVisible = visibleBounds[1][1];
        
        int idx = firstVisible;
        int x = getViewX(contx, xyItem, idx);
        int dist = Math.abs(viewX - x);
        
        while (++idx <= lastVisible) {
            int newX = getViewX(contx, xyItem, idx);
            int newDist = Math.abs(viewX - newX);
            if (newDist > dist) {
                idx--;
                break;
            } else {
                x = newX;
                dist = newDist;
            }
        }
        
        if (idx > lastVisible) idx = lastVisible;
        
        long maxVal = xyItem.getYValue(idx);
        int maxIdx = idx;
        
        while (--idx >= firstVisible && getViewX(contx, xyItem, idx) == x) {
            long y = xyItem.getYValue(idx);
            if (y > maxVal) {
                maxVal = y;
                maxIdx = idx;
            }
        }
        
        return new XYItemSelection.Default(xyItem, maxIdx, dist);
    }
    
    private int[][] getFastPoints(XYItem item, Rectangle dirtyArea,
                                  SynchronousXYChartContext context,
                                  int type, int maxValueOffset) {

        int valuesCount = item.getValuesCount();
        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);

        int firstFirst = visibleBounds[0][0];
        int firstIndex = firstFirst;
        if (firstIndex == -1) firstIndex = visibleBounds[0][1];
        if (firstIndex == -1) return null;
        if (firstFirst != -1 && firstIndex > 0) firstIndex -= 1;

        int lastFirst = visibleBounds[1][0];
        int lastIndex = lastFirst;
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        if (lastIndex == -1) lastIndex = valuesCount - 1;
        if (lastFirst != -1 && lastIndex < valuesCount - 1) lastIndex += 1;

        int itemsStep = (int)(valuesCount / context.getViewWidth());
        if (itemsStep == 0) itemsStep = 1;

        int visibleCount = lastIndex - firstIndex + 1;

        if (itemsStep > 1) {
            int firstMod = firstIndex % itemsStep;
            firstIndex -= firstMod;
            int lastMod = lastIndex % itemsStep;
            lastIndex = lastIndex - lastMod + itemsStep;
            visibleCount = (lastIndex - firstIndex) / itemsStep + 1;
            lastIndex = Math.min(lastIndex, valuesCount - 1);
        }

        int[] xPoints = new int[visibleCount + 2];
        int[] yPoints = new int[visibleCount + 2];


        double itemValueFactor = type == TYPE_RELATIVE ? getItemValueFactor(context,
                                 maxValueOffset, item.getBounds().height) : 0;

        for (int i = 0; i < visibleCount; i++) {
            int dataIndex = i == visibleCount - 1 ? lastIndex :
                                 firstIndex + i * itemsStep;
            xPoints[i] = Utils.checkedInt(Math.ceil(
                         context.getViewX(item.getXValue(dataIndex))));
            yPoints[i] = Utils.checkedInt(Math.ceil(
                         getYValue(item, dataIndex,
                         type, context, itemValueFactor)));
        }

        return new int[][] { xPoints, yPoints, { xPoints.length - 2 } };
    }
    
    private int[][] getMinMaxPoints(XYItem item, Rectangle dirtyArea,
                                    SynchronousXYChartContext context,
                                    int type, int maxValueOffset) {
        
        if (dirtyArea.isEmpty()) return null;
        
        dirtyArea.grow(lineWidth, 0);
        
        int[][] visibleBounds = context.getVisibleBounds(dirtyArea);
        if (visibleBounds[0][0] == -1 && visibleBounds[0][1] == -1) return null;
        else if (visibleBounds[1][0] == -1 && visibleBounds[1][1] == -1) return null;
        
        int valuesCount = item.getValuesCount();
        
        int firstIndex = visibleBounds[0][0];
        if (firstIndex == -1) firstIndex = visibleBounds[0][1];
        else if (firstIndex > 0) firstIndex--; // must use previous point to draw first line
        
        int lastIndex = visibleBounds[1][0];
        if (lastIndex == -1) lastIndex = visibleBounds[1][1];
        else if (lastIndex < valuesCount - 1) lastIndex++; // must use next point to draw last line
        
//        int firstX = getViewX(context, item, firstIndex);
//        while (firstIndex > 0 && getViewX(context, item, firstIndex) >= firstX - lineWidth)
//            firstIndex--;
//        
//        int lastX = getViewX(context, item, lastIndex);
//        while (lastIndex < valuesCount - 1 && getViewX(context, item, lastIndex) <= lastX + lineWidth)
//            lastIndex++;
        
        double itemValueFactor = type == TYPE_RELATIVE ? getItemValueFactor(context,
                                 maxValueOffset, item.getBounds().height) : 0;
        
//        int maxPoints = Math.min((lineWidth + dirtyArea.width + lineWidth) * 4, lastIndex - firstIndex + 1);
        int maxPoints = Math.min(dirtyArea.width * 4 + 2, lastIndex - firstIndex + 1); // +2 for the extra invisible first & last points
        
        int[] xPoints = new int[maxPoints + 2];
        int[] yPoints = new int[maxPoints + 2];
        
        int nPoints = 0;
        for (int index = firstIndex; index <= lastIndex; index++) {
            int x = getViewX(context, item, index);
            int y = Utils.checkedInt(Math.ceil(getYValue(item, index,
                                     type, context, itemValueFactor)));
            
            int nValues = 0;
            
            if (nPoints > 0) {
                if (xPoints[nPoints - 1] == x) nValues = 1;
                
                if (nPoints > 1) {
                    if (xPoints[nPoints - 2] == x) nValues = 2;
                    
                    if (nPoints > 2) {
                        if (xPoints[nPoints - 3] == x) nValues = 3;
                        
                        if (nPoints > 3) {
                            if (xPoints[nPoints - 4] == x) nValues = 4;
                        }
                    }
                }
            }
            
            switch (nValues) {
                // New point at X
                case 0:
                    if (nPoints < 2 || yPoints[nPoints - 1] != y || yPoints[nPoints - 2] != y) { // first, second or new point, create it
                        xPoints[nPoints] = x;
                        yPoints[nPoints] = y;
                        nPoints++;
                    } else { // repeated point, collapse it
                        xPoints[nPoints - 1] = x;
                    }
                    
                    break;
                
                // Second point at X
                case 1:
                    if (yPoints[nPoints - 1] != y) { // only add second point if its value differs from the first point
                        xPoints[nPoints] = x;
                        yPoints[nPoints] = y;
                        nPoints++;
                    }
                    
                    break;
                
                // Third point at X
                case 2:
                    int y_1_2 = yPoints[nPoints - 1];
                    if (y_1_2 != y) { // only add third point if its value differs from the second point
                        if (yPoints[nPoints - 2] < y_1_2 && y_1_2 < y) { // new maximum value, collapse it
                            yPoints[nPoints - 1] = y;
                        } else if (yPoints[nPoints - 2] > y_1_2 && y_1_2 > y) { // new minimum value, collapse it
                            yPoints[nPoints - 1] = y;
                        } else { // new end value, create it
                            xPoints[nPoints] = x;
                            yPoints[nPoints] = y;
                            nPoints++;
                        }
                    }
                    
                    break;
                
                // Fourth point at X
                case 3:
                    int y_1_3 = yPoints[nPoints - 1];
                    if (y_1_3 != y) { // only add fourth point if its value differs from the third point
                        int y_2_3 = yPoints[nPoints - 2];
                        int y_3_3 = yPoints[nPoints - 3];
                        
                        int min = y;
                        int max = y;
                        
                        if (y_1_3 < min) min = y_1_3;
                        else if (y_1_3 > max) max = y_1_3;
                        
                        if (y_2_3 < min) min = y_2_3;
                        else if (y_2_3 > max) max = y_2_3;
                        
                        if (y_3_3 < min) min = y_3_3;
                        else if (y_3_3 > max) max = y_3_3;
                        
                        if (y == min) {
                            if (y_3_3 == max) {
                                yPoints[nPoints - 2] = y;
                                nPoints--;
                            } else {
                                yPoints[nPoints - 2] = max;
                                yPoints[nPoints - 1] = y;
                            }
                        } else if (y == max) {
                            if (y_3_3 == min) {
                                yPoints[nPoints - 2] = y;
                                nPoints--;
                            } else {
                                yPoints[nPoints - 2] = min;
                                yPoints[nPoints - 1] = y;
                            }
                        } else if (y_3_3 == min) {
                            yPoints[nPoints - 2] = max;
                            yPoints[nPoints - 1] = y;
                        } else if (y_3_3 == max) {
                            yPoints[nPoints - 2] = min;
                            yPoints[nPoints - 1] = y;
                        } else {
                            xPoints[nPoints] = x;
                            yPoints[nPoints] = y;
                            nPoints++;
                        }
                    }
                    
                    break;
                
                // Another point at X
                case 4:
                    int y_1_4 = yPoints[nPoints - 1];
                    if (y_1_4 != y) { // only add another point if its value differs from the fourth point
                        int y_2_4 = yPoints[nPoints - 2];
                        int y_3_4 = yPoints[nPoints - 3];
                        int y_4_4 = yPoints[nPoints - 4];
                        
                        int min = y;
                        int max = y;
                        
                        if (y_1_4 < min) min = y_1_4;
                        else if (y_1_4 > max) max = y_1_4;
                        
                        if (y_2_4 < min) min = y_2_4;
                        else if (y_2_4 > max) max = y_2_4;
                        
                        if (y_3_4 < min) min = y_3_4;
                        else if (y_3_4 > max) max = y_3_4;
                        
                        if (y_4_4 < min) min = y_4_4;
                        else if (y_4_4 > max) max = y_4_4;
                        
                        if (y == min) {
                            yPoints[nPoints - 3] = max;
                            yPoints[nPoints - 2] = y;
                            nPoints--;
                        } else if (y == max) {
                            yPoints[nPoints - 3] = min;
                            yPoints[nPoints - 2] = y;
                            nPoints--;
                        } else {
                            yPoints[nPoints - 1] = y;
                        }
                    }
            }
        }
        
        return new int[][] { xPoints, yPoints, { nPoints } };
    }
    
    private static int getViewX(SynchronousXYChartContext context, XYItem item, int index) {
        return Utils.checkedInt(Math.ceil(context.getViewX(item.getXValue(index))));
    }

    private LongRect getViewBoundsRelative(LongRect dataBounds, XYItem item,
                                           ChartContext context) {
        LongRect itemBounds = item.getBounds();

        double itemValueFactor = getItemValueFactor(context, maxValueOffset,
                                                    itemBounds.height);

        // TODO: fix the math!!!
        double value1 = context.getDataOffsetY() + itemValueFactor *
                      (double)(dataBounds.y - itemBounds.y);
        double value2 = context.getDataOffsetY() + itemValueFactor *
                      (double)(dataBounds.y + dataBounds.height - itemBounds.y);

        long viewX = (long)Math.ceil(context.getViewX(dataBounds.x));
        long viewWidth = (long)Math.ceil(context.getViewWidth(dataBounds.width));
        if (context.isRightBased()) viewX -= viewWidth;

        long viewY1 = (long)Math.ceil(context.getViewY(value1));
        long viewY2 = (long)Math.ceil(context.getViewY(value2));
        long viewHeight = context.isBottomBased() ? viewY1 - viewY2 :
                                                    viewY2 - viewY1;
        if (!context.isBottomBased()) viewY2 -= viewHeight;

        LongRect viewBounds =  new LongRect(viewX, viewY2, viewWidth, viewHeight);
        LongRect.addBorder(viewBounds, lineWidth);

        return viewBounds;
    }

    private LongRect getViewBounds(XYItem item, int[] valuesIndexes, ChartContext context) {

        LongRect dataBounds = new LongRect();

        if (valuesIndexes == null) {
            LongRect.set(dataBounds, item.getBounds());
        } else {
            boolean firstPoint = true;
            for (int valueIndex : valuesIndexes) {
                if (valueIndex == -1) continue;
                long xValue = item.getXValue(valueIndex);
                long yValue = item.getYValue(valueIndex);
                if (firstPoint) {
                    LongRect.set(dataBounds, xValue, yValue, 0, 0);
                    firstPoint = false;
                } else {
                    LongRect.add(dataBounds, xValue, yValue);
                }
            }
        }

        if (type == TYPE_RELATIVE) {

            return getViewBoundsRelative(dataBounds, item, context);

        } else {

            LongRect viewBounds = context.getViewRect(dataBounds);
            LongRect.addBorder(viewBounds, lineWidth);
            return viewBounds;

        }
    }

    private static double getItemValueFactor(ChartContext context,
                                             double maxValueOffset,
                                             double itemHeight) {
        return ((double)context.getDataHeight() -
               context.getDataHeight(maxValueOffset)) / itemHeight;
    }

    private static double getYValue(XYItem item, int valueIndex,
                                  int type, ChartContext context, double itemValueFactor) {
        if (type == TYPE_ABSOLUTE) {
            return context.getViewY(item.getYValue(valueIndex));
        } else {
            return context.getViewY(context.getDataOffsetY() + (itemValueFactor *
                        (item.getYValue(valueIndex) - item.getBounds().y)));
        }
    }

}
