package com.gonichiwa.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.gonichiwa.model.MindMapModel;
import com.gonichiwa.model.MindMapNode;
import com.gonichiwa.util.MindMapVector;

public class MindMapGraphView extends JPanel implements Observer {

	private MindMapModel model;
	private ArrayList<MindMapNodeView> nodes;
	private ArrayList<MindMapEdge> edges;
	private MouseAdapter nodeMouseListener;
	private KeyListener nodeKeyListener;

	private JLabel label;
	private double zoomFactor = 1;
	private double dx, dy;


	public MindMapGraphView(MindMapModel model, int width, int height) {
		this.model = model;
		this.model.tree.addObserver(this);
		this.setLayout(null);
		this.setPreferredSize(new Dimension(width, height));
		nodes = new ArrayList<MindMapNodeView>();
		edges = new ArrayList<MindMapEdge>();
		this.setFocusable(true);
		this.setRequestFocusEnabled(true); 		// now we can request this panel for focus.
	}

	public double getZoomFactor() {
		return zoomFactor;
	}



	public void setZoomFactor(double zoomFactor) {
		this.zoomFactor = zoomFactor;
	}


	public void reset() {
		this.clearNodes();
		this.removeAll();
		this.repaint();
	}

	public void addNodeMouseAdapter(MouseAdapter l) {
		nodeMouseListener = l;
	}

	public void addNodeKeyListener(KeyListener l) {
		nodeKeyListener = l;
	}

	public void addNode(MindMapNodeView node) {
		nodes.add(node);
		add(node);
		this.repaint();
	}

	public void removeNode(MindMapNodeView node) {
		nodes.remove(node);
		remove(node);
	}

	public void clearNodes() {
		nodes.clear();
		edges.clear();
		this.removeAll();
	}

	public void addMouseListenerToNodes(MouseListener l) {
		for(MindMapNodeView node : nodes)
			node.addMouseListener(l);
	}

	public void addMouseMotionListenerToNodes(MouseMotionListener l) {
		for(MindMapNodeView node : nodes)
			node.addMouseMotionListener(l);
	}

//	@Override
//	public void repaint() {
//		super.repaint();
//		revalidate();
//		repaintAllNodes();
//	}

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		if(o instanceof MindMapNode) {
			this.repaint();
			return;
		}
		
		switch(String.valueOf(arg)) {
		case "NEW":
			drawGraph();
			break;
		case "LOAD":

			loadGraph();
			break;
		default:
			break;
		}
	}

//	public void repaintAllNodes() {
//		for(MindMapNodeView node: nodes) {
//			node.repaint();
//			node.revalidate();
//		}
//	}

	public void drawGraph() {
		this.clearNodes();
		this.repaint();
		this.revalidate();
		System.out.println("tree size is" + model.tree);
		recMakeNodeView(model.tree.getRoot(), this.getPreferredSize().width/2, this.getPreferredSize().height/2, Math.PI*2, new MindMapVector(0, -1), 1);
	}

	/**
	 * load Graph from already built tree.
	 */
	public void loadGraph() {

		this.clearNodes();
		recLoadNode(model.tree.getRoot());
		this.repaint();
		this.revalidate();
	}

	private void recLoadNode(MindMapNode node) {

		MindMapNodeView nodeView = new MindMapNodeView(node);
		nodeView.addMouseListener(nodeMouseListener);
		nodeView.addMouseMotionListener(nodeMouseListener);
		nodeView.addKeyListener(nodeKeyListener);
		addNode(nodeView);
		nodeView.zoomNode(zoomFactor, 0, 0);
		nodeView.moveNode((int)dx, (int)dy);
		node.addObserver(this);
		for(MindMapNode child : node.getChildren()) {
			recLoadNode(child);
			edges.add(new MindMapEdge(node, child));
		}
	}

	private void recMakeNodeView(MindMapNode node, int centerX, int centerY, double availableAngle, MindMapVector direction, int colorLevel) {
		// make node first
		// TODO: we might need more better algorithm here.
		// TODO: using node size not constant.

		// for example, angle decision.
		int numberOfChildren = 0;
		double theta = 0;
		double distance = 0;

		// make NodeView

		System.out.println(node.getName() + " is making...");
		MindMapNodeView nodeView = new MindMapNodeView(node, centerX, centerY, new Color(0 + colorLevel, 0, 255-colorLevel));
		nodeView.addMouseListener(nodeMouseListener);
		nodeView.addMouseMotionListener(nodeMouseListener);
		nodeView.addKeyListener(nodeKeyListener);
		
		System.out.println(nodeView.getLocation().x + " " + nodeView.getLocation().y);
		node.initViewAttribute(nodeView.getX(),
							   nodeView.getY(),
							   nodeView.getPreferredSize().width, 
							   nodeView.getPreferredSize().height,
							   nodeView.getColor().getRed(),
							   nodeView.getColor().getGreen(),
							   nodeView.getColor().getBlue());
		node.addObserver(this);
		
		// node.setColor()
		this.addNode(nodeView);
		nodeView.zoomNode(zoomFactor, 0, 0);
		nodeView.moveNode((int)dx, (int)dy);

		// get number of children
		numberOfChildren = node.getChildren().size();

		// get theta
		if(availableAngle != 2*Math.PI)
			theta = availableAngle / (numberOfChildren-1);
		else
			theta = availableAngle / numberOfChildren;

		// get distance.
		distance = (numberOfChildren > 1) ? MindMapNodeView.MIN_SIZE/Math.sin(theta/2) : 40; // 10 is debug offset.

		direction.normalize();
		System.out.println(node.getName() + "'s direction is " + direction.getX() +", "+ direction.getY());
		direction.mult(distance);

		if(theta > Math.PI)
			theta = Math.PI/2;

		for(MindMapNode child : node.getChildren()) {

			recMakeNodeView(child,
						 centerX+(int)direction.getX(),
						 centerY+(int)direction.getY(),
						 theta,
						 direction.copy().normalize().rotate(-theta/2), colorLevel + 50);
			edges.add(new MindMapEdge(node, child));
			direction.rotate(theta);
		}
	}

	
	public void zoom(int x, int y, double factor) {
		dx = (int) ((dx - x) * (factor / zoomFactor) + x);
		dy = (int) ((dy - y) * (factor / zoomFactor) + y);
		setZoomFactor(factor);

//		zoomable = true;
//		//		zoomX = (x - dx + this.getWidth() / zoomFactor);
//		//		zoomY = (y - dy + this.getHeight() / zoomFactor);
//		zoomX = x / zoomFactor - dx;
//		zoomY = y / zoomFactor - dy;
//		System.out.println("before zoomX " + zoomX);

		for(MindMapNodeView node : nodes) {
			System.out.println("factor " + factor);
			node.zoomNode(factor, x, y);
		}
		this.repaint();
		this.revalidate();
		this.doLayout();
	}

	public void movePanel(double dx, double dy) {
		this.dx += dx;
		this.dy += dy;
		for(MindMapNodeView node : nodes) {
			node.moveNode((int)dx,  (int)dy);
		}
		this.repaint();
//		this.revalidate();
//		this.doLayout();
	}

	public double getDX() {
		return dx;
	}

	public double getDY() {
		return dy;
	}
	
	@Override 
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		AffineTransform at = g2d.getTransform();
		System.out.println(dx + " " + dy);
//	    at.setToTranslation(dx, dy);
//		at.scale(zoomFactor, zoomFactor);
//		at.translate((zoomX / zoomFactor - zoomX), (zoomY / zoomFactor - zoomY));
//	    g2d.setTransform(at);
		System.out.println("dx is " + dx + " " + dy);
		int idx = (int) dx;
		int idy = (int) dy;
		QuadCurve2D q2 = new QuadCurve2D.Float();
		g2d.setStroke(new BasicStroke(1));

		// is that ok for each NodeView to have parent information?
		// isn't that domain logic? or is it view logic?
		for(MindMapEdge edge : edges) {
			g2d.drawLine(edge.getX1(),
						 edge.getY1(), 
						 edge.getX2(),
						 edge.getY2());
//			q2.setCurve(edge.x1, edge.y1, 0, 0, edge.x2, edge.y2);
//			g2d.draw(q2);
		}
		this.paintChildren(g2d);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;

	    AffineTransform at = g2d.getTransform();
//
//	    at.setToTranslation(dx, dy);
//
//	    at.scale(zoomFactor, zoomFactor);
//	    System.out.println("x translate after zoom : " + (zoomX / zoomFactor - zoomX));
//	    at.translate((zoomX / zoomFactor - zoomX), (zoomY / zoomFactor - zoomY));
//	    at.translate(-zoomX * (zoomFactor - 1), -zoomY * (zoomFactor - 1));
//	    System.out.println(zoomX + " " + zoomY);

	    g2d.setTransform(at);
	    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	}
	
	private MindMapNodeView getNodeView(int nodeID) {
		for(MindMapNodeView node : nodes) {
			if(node.getID() == nodeID) {
				return node;
			}
		}
		return null;
	}

	private class MindMapEdge {
		private MindMapNodeView from;
		private MindMapNodeView to;
		private int offsetX, offsetY;
		private double zoomFactor;
		
		public MindMapEdge(MindMapNode from, MindMapNode to) {
			this.from = getNodeView(from.getID());
			this.to = getNodeView(to.getID());
		}

		public int getX1() {
			return (int) (from.getRelativeX() + (from.getWidth()/2));
		}

		public int getX2() {
			return (int) (to.getRelativeX() + (to.getWidth()/2));
		}

		public int getY1() {
			return (int) (from.getRelativeY() + (from.getHeight()/2));
		}

		public int getY2() {
			return (int) (to.getRelativeY() + (to.getHeight()/2));
		}
		
		
	}


}
