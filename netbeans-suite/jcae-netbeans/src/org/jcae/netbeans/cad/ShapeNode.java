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

package org.jcae.netbeans.cad;

import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.Action;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.actions.DeleteAction;
import org.openide.actions.ViewAction;
import org.openide.nodes.AbstractNode;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.NewType;

public class ShapeNode extends AbstractNode implements ShapeCookie
{	
	private String name;
	
	protected TopoDS_Shape shape;
	
	public ShapeNode(String name, TopoDS_Shape shape, ShapePool pool)
	{		
		super(new ShapeChildren());
		this.shape=shape;
		getCookieSet().add(this);
		getCookieSet().add(pool);
		getCookieSet().add(new ShapeOperationCookie(this));
		getCookieSet().add((Cookie) getChildren());
		this.name=name;
		pool.putNode(shape, this);
	}
	
	public Action[] getActions(boolean arg0)
	{
		ArrayList toReturn=new ArrayList();		
		toReturn.add(SystemAction.get(ExplodeAction.class));
		toReturn.add(SystemAction.get(ViewAction.class));
		toReturn.add(SystemAction.get(DeleteAction.class));
		toReturn.add(SystemAction.get(BooleanAction.AllActions.class));
		toReturn.add(SystemAction.get(TransformAction.AllActions.class));
		toReturn.add(SystemAction.get(SewAction.class));
		toReturn.add(SystemAction.get(GroupFaceAction.class));
		toReturn.add(SystemAction.get(FreeBoundsAction.class));
		toReturn.add(SystemAction.get(BoundingBoxAction.class));
		if(getShape().shapeType()==TopAbs_ShapeEnum.FACE)
			toReturn.add(SystemAction.get(ReverseAction.class));

		toReturn.addAll(Arrays.asList(super.getActions(arg0)));
		return (Action[]) toReturn.toArray(new Action[0]); 
	}

	public String getDisplayName()
	{
		return name;
	}

	public String getName()
	{
		return name;
	}
	
	public TopoDS_Shape getShape()
	{
		return shape;
	}

	public boolean canDestroy()
	{
		return true;
	}
	
	/*public void destroy() throws IOException
	{
		ShapePool sp=(ShapePool) getCookie(ShapePool.class);
		getParentNode().getChildren().remove(new Node[]{this});
	}*/
	
	public NewType[] getNewTypes()
	{
		return PrimitiveNewType.getNewType(this);		
	}

}
