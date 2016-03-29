package de.uni_potsdam.hpi.asg.logictool.srgraph;

/*
 * Copyright (C) 2012 - 2014 Norman Kluge
 * 
 * This file is part of ASGlogic.
 * 
 * ASGlogic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ASGlogic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ASGlogic.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;

import javax.swing.Action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxCellRenderer.CanvasFactory;
import com.mxgraph.util.mxDomUtils;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.util.png.mxPngEncodeParam;
import com.mxgraph.util.png.mxPngImageEncoder;
import com.mxgraph.view.mxGraph;

public class ExportAction implements Action {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if(arg0.getSource() instanceof mxGraphComponent) {
            mxGraphComponent comp = (mxGraphComponent)arg0.getSource();
            mxGraph graph = comp.getGraph();
            if(exportSvg(graph, new File("screen.svg"))) {
                logger.info("SVG export successful");
            } else {
                logger.error("SVG export unsuccessful");
            }
            if(exportPng(new File("screen.png"), graph, comp)) {
                logger.info("PNG export successful");
            } else {
                logger.error("PNG export unsuccessful");
            }

        } else {
            System.out.println("error");
        }
    }

    @Override
    public void setEnabled(boolean arg0) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener arg0) {
    }

    @Override
    public void putValue(String arg0, Object arg1) {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Object getValue(String arg0) {
        return null;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener arg0) {
    }

    public boolean exportSvg(mxGraph graph, File file) {
        mxSvgCanvas canvas = (mxSvgCanvas)mxCellRenderer.drawCells(graph, null, 1, null, new CanvasFactory() {
            @Override
            public mxICanvas createCanvas(int width, int height) {
                mxSvgCanvas canvas = new mxSvgCanvas(mxDomUtils.createSvgDocument(width, height));
                canvas.setEmbedded(true);
                return canvas;
            }
        });

        try {
            mxUtils.writeFile(mxXmlUtils.getXml(canvas.getDocument()), file.getAbsolutePath());
        } catch(IOException e) {
            logger.error(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public boolean exportPng(File file, mxGraph graph, mxGraphComponent graphComponent) {
        try {
            BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, graphComponent.isAntiAlias(), null, graphComponent.getCanvas());
            mxCodec codec = new mxCodec();
            String xml = URLEncoder.encode(mxXmlUtils.getXml(codec.encode(graph.getModel())), "UTF-8");
            mxPngEncodeParam param = mxPngEncodeParam.getDefaultEncodeParam(image);
            param.setCompressedText(new String[]{"mxGraphModel", xml});
            FileOutputStream outputStream = new FileOutputStream(file);
            mxPngImageEncoder encoder = new mxPngImageEncoder(outputStream, param);
            if(image != null) {
                encoder.encode(image);
            } else {
                logger.error("No Image");
            }
            outputStream.close();
            return true;
        } catch(IOException e) {
            logger.error(e.getLocalizedMessage());
            return false;
        } catch(OutOfMemoryError e) {
            logger.error(e.getLocalizedMessage());
            return false;
        }
    }
}
