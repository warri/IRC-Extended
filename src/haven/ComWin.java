/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;

public class ComWin extends HWindow {
    int ip = 0;
    long atkc = -1;
    Indir<Resource> blk, batk, iatk;
    static Tex iptex = Resource.loadtex("gfx/hud/combat/ip");
    Text iptext = Text.render("Initiative: ", Color.BLACK);
    
    static {
	Widget.addtype("comwin", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new ComWin(parent));
		}
	    });
    }
    
    public ComWin(Widget parent) {
	super(parent, "Combat", false);
	(new Label(new Coord(10, 5), this, "Attack:")).setcolor(Color.BLACK);
	new Label(new Coord(10, 55), this, "Maneuver:").setcolor(Color.BLACK);
    }
    
    public void draw(GOut g) {
	super.draw(g);
	Resource res;
	boolean hasbatk = (batk != null) && (batk.get() != null);
	boolean hasiatk = (iatk != null) && (iatk.get() != null);
	if(hasbatk) {
	    res = batk.get();
	    g.image(res.layer(Resource.imgc).tex(), new Coord(15, 20));
	    if(!hasiatk) {
		g.chcolor(0, 0, 0, 255);
		g.atext(res.layer(Resource.action).name, new Coord(50, 35), 0, 0.5);
		g.chcolor();
	    }
	}
	if(hasiatk) {
	    res = iatk.get();
	    Coord c;
	    if(hasbatk)
		c = new Coord(18, 23);
	    else
		c = new Coord(15, 20);
	    g.image(res.layer(Resource.imgc).tex(), c);
	    g.chcolor(0, 0, 0, 255);
	    g.atext(res.layer(Resource.action).name, new Coord(50, 35), 0, 0.5);
	    g.chcolor();
	}
	if((blk != null) && ((res = blk.get()) != null)) {
	    g.image(res.layer(Resource.imgc).tex(), new Coord(15, 70));
	    g.chcolor(0, 0, 0, 255);
	    g.atext(res.layer(Resource.action).name, new Coord(50, 85), 0, 0.5);
	    g.chcolor();
	}
	g.image(iptext.tex(), new Coord(200, 30));
	for(int i = 0; i < ip; i++)
	    g.image(iptex, new Coord(200 + iptext.sz().x + (i * 4), 32));
	long now = System.currentTimeMillis();
	if(now < atkc) {
	    g.chcolor(255, 0, 128, 255);
	    g.frect(new Coord(200, 55), new Coord((int)(atkc - now) / 100, 20));
	    g.chcolor();
	}
    }
    
    private Indir<Resource> n2r(int num) {
	if(num < 0)
	    return(null);
	return(ui.sess.getres(num));
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "ip") {
	    ip = (Integer)args[0];
	} else if(msg == "blk") {
	    blk = n2r((Integer)args[0]);
	} else if(msg == "atk") {
	    batk = n2r((Integer)args[0]);
	    iatk = n2r((Integer)args[1]);
	} else if(msg == "atkc") {
	    atkc = System.currentTimeMillis() + (((Integer)args[0]) * 60);
	} else {
	    super.uimsg(msg, args);
	}
    }
}
