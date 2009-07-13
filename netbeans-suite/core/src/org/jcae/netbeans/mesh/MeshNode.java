/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.netbeans.mesh;

import java.awt.datatransfer.Transferable;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.xmldata.Storage;
import org.jcae.mesh.xmldata.Group;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.mesh.xmldata.GroupsReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.netbeans.BeanProperty;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.cad.BrepNode;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.AmibeToMesh;
import org.jcae.vtk.View;
import org.jcae.vtk.ViewableMesh;
import org.openide.ErrorManager;
import org.openide.actions.*;
import org.openide.cookies.ViewCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.nodes.Node.PropertySet;
import org.openide.nodes.NodeTransfer;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.xml.sax.SAXException;

/**
 * Contains only the geometric node and the groups node.
 * Lookups :
 * this.
 * @author ibarz
 */
public class MeshNode extends DataNode implements ViewCookie
{
	private Groups groups;
	private AbstractNode groupsNode;
	private AbstractNode subMeshNode;
	
	public MeshNode(DataObject arg0)
	{
		super(arg0, new Children.Array());	
		getCookieSet().add(this);
		updateSubmeshNode();
		refreshGroups();
	}

	public AbstractNode getGroupsNode()
	{
		return groupsNode;
	}


	protected Property[] getExpertProperties()
	{
		try
		{
			return new Property[]{
				new BeanProperty(getMesh(), "geometryFile"),
				new BeanProperty(getMesh(), "meshFile"),
				new BeanProperty(getMesh(), "boraFile")
			};
		}
		catch (NoSuchMethodException e)
		{
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		}
		catch (IntrospectionException e) {
			ErrorManager.getDefault().notify(e);
			return new Property[0];
		}
	}
	
	@Override
	public PropertySet[] getPropertySets() {
		return new PropertySet[]{
					new PropertySet() {

						public Property[] getProperties() {
							return MeshNode.this.getExpertProperties();
						}

						@Override
						public String getName() {
							return "Expert";
						}

						@Override
						public boolean isExpert() {
							return true;
						}
					}
				};
	}

	/**
	 * Display in a View3D a list of groups.
	 * The xml Directory xmlDir must be setted. After having load a project, xmlDir may be null the first time.
	 *
	 * @param the list of groups to display.
	 * @param the View3D in which the Groups are displayed.
	 */
	public static void displayGroups(Groups groups, String meshName,
		Collection<Group> groupsToDisplay, View view)
		throws ParserConfigurationException, SAXException, IOException
	{
		int[] idGroupsDisplayed = new int[groupsToDisplay.size()];
		Iterator<Group> iter = groupsToDisplay.iterator();
		String sb="";
		boolean full=false;
		for(int i = 0 ; i < idGroupsDisplayed.length ; ++i)
		{
			Group g = iter.next();

			idGroupsDisplayed[i] = g.getId();
			if(sb.length()<20)
				sb=sb+" "+g.getName();
			else
				full=true;
		}

		if(full)
			sb=sb+"...";

		AmibeToMesh reader = new AmibeToMesh(groups.getMeshFile(), idGroupsDisplayed);
		ViewableMesh interactor = new ViewableMesh(reader.getMesh());
		interactor.setName(meshName+" ["+sb+"]");
		SelectionManager.getDefault().addInteractor(interactor, groups);
		view.add(interactor);
	}

	public void view() {
		if (subMeshNode != null && getMesh() != null && getMesh().getBoraModel() != null) {
			try {
				MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
				mtb.addNodeList();
				org.jcae.mesh.amibe.ds.Mesh m = new org.jcae.mesh.amibe.ds.Mesh(
						mtb);
				Storage.readAllFaces(m,
						getMesh().getBoraModel().getGraph().getRootCell());
				MeshWriter.writeObject3D(m, getMeshDirectory(), "dummy.brep");
				View v = ViewManager.getDefault().getCurrentView();
				AmibeToMesh toMesh = new AmibeToMesh(getMeshDirectory());
				ViewableMesh vMesh = new ViewableMesh(toMesh.getMesh());
				vMesh.setName(subMeshNode.getDisplayName()+" mesh");
				v.add(vMesh);
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}
		}
	}

	public String getMeshDirectory() {
		String ref=FileUtil.toFile(getDataObject().getPrimaryFile().getParent()).getPath();
		return Utilities.absoluteFileName(getMesh().getMeshFile(), ref);		
	}
	/**
	 * @return Returns the groups
	 */
	public void refreshGroups()
	{
		String meshDir=getMeshDirectory();
		File xmlFile=new File(meshDir, "jcae3d");
        if (xmlFile.exists())
            groups = GroupsReader.getGroups(meshDir);
        
        if(groupsNode!=null)
        {
        	getChildren().remove(new Node[]{groupsNode});
        }
        
        if(groups!=null && groups.getGroups().length>0)
        {
			GroupChildren groupChildren = new GroupChildren(groups);
        	groupsNode=new AbstractNode(groupChildren, Lookups.fixed(groupChildren));
        	groupsNode.setDisplayName("Groups");
        	getChildren().add(new Node[]{groupsNode});
        }
	}

	public Mesh getMesh() {
		return getCookie(MeshDataObject.class).getMesh();
	}
	
	@Override
	public String getHtmlDisplayName() {
		return "<font color='0000FF'>" + getName() + "</font>";
	}

	@Override
	public void setName(String arg0)
	{	
		try
		{
			String o=getName();
			getDataObject().rename(arg0+"_meshBora");
			fireDisplayNameChange(o, arg0);
			fireNameChange(o, arg0);
		}
		catch (IOException e)
		{
			ErrorManager.getDefault().notify(e);
		}
	}
	
	@Override
	public String getName()
	{
		String s = getDataObject().getName();
		return s.substring(0, s.length()-"_meshBora".length());
	}
	

	/**
	 * Indicate if groups is the same than has the groupNode
	 * @param groups
	 */
	boolean hasThisGroupsNode(Groups groups)
	{
		return this.groups == groups;
	}
	
	@Override
	protected void createPasteTypes(Transferable t, List<PasteType> ls)
	{
		final Node[] ns = NodeTransfer.nodes(t, NodeTransfer.COPY|NodeTransfer.MOVE);
		if (ns != null && ns.length==1) {
			final BrepNode n=ns[0].getCookie(BrepNode.class);
			if(n!=null)
			ls.add(new PasteType()
			{
				public Transferable paste()
				{
					getMesh().setGeometryFile(n.getDataObject().getPrimaryFile().getNameExt());
					String geomPath = n.getDataObject().getPrimaryFile().getPath();
					geomPath = "/" + geomPath;
					String outPath =  geomPath.substring(0, geomPath.lastIndexOf("/"));
					getMesh().setBoraModel(new BModel(geomPath,outPath));
					getMesh().setBoraFile(outPath+ getMesh().getBoraModel().getOutputFile());
					getMesh().getBoraModel().newMesh();
					getMesh().getBoraModel().save(); //writing the file to disk.
					updateSubmeshNode();
					firePropertyChange(null, null, null);
					return null;
				}
			});
		}
		// Also try superclass, but give it lower priority:
		super.createPasteTypes(t, ls);
	}
	
	private void updateSubmeshNode()
	{
		if(subMeshNode!=null) {
			getChildren().remove(new Node[]{subMeshNode});
			try {
				subMeshNode.destroy();
			}
			catch (IOException io) {
				io.printStackTrace();
			}
		}
		if (getMesh().getBoraModel() != null) {
			subMeshNode = new SubmeshNode(getMesh().getGeometryFile().substring(
					0, getMesh().getGeometryFile().lastIndexOf(".")), getMesh().getBoraModel());
			getChildren().add(new Node[] { subMeshNode } );
		}
			
	}
		
	@Override
	public Action getPreferredAction()
	{
		return SystemAction.get(PropertiesAction.class);
	}
	
	@Override
	public Action[] getActions(boolean b)
	{		
		Action[] actions=super.getActions(b);
		Action[] toReturn=new Action[actions.length+1];
		System.arraycopy(actions, 0, toReturn, 0, actions.length);
		toReturn[actions.length]=SystemAction.get(ExpertMenu.class);
		return toReturn;
	}	
}