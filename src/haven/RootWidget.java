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

import java.awt.GraphicsConfiguration;
import java.awt.event.KeyEvent;

public class RootWidget extends Widget {
    Logout logout = null;
    Profile gprof;
    GameOptions opts;
    boolean afk = false;

    public RootWidget(UI ui, Coord sz) {
	super(ui, new Coord(0, 0), sz);
	setfocusctl(true);
	cursor = Resource.load("gfx/hud/curs/arw");
    }

    public boolean globtype(char key, KeyEvent ev) {
		if(!super.globtype(key, ev)) {
	    /*
	      if(key == 27) {
	      if(logout == null) {
	      if(ui.sess != null)
	      logout = new Logout(new Coord(338, 275), this) {
	      public void destroy() {
	      super.destroy();
	      logout = null;
	      }
	      };
	      } else {
	      ui.destroy(logout);
	      logout = null;
	      }
	      } else */
	    if(Config.profile && (key == '`')) {
		new Profwnd(findchild(SlenHud.class), findchild(MapView.class).prof, "MV prof");
	    } else if(Config.profile && (key == '~')) {
		new Profwnd(findchild(SlenHud.class), gprof, "Glob prof");
	    } else if(Config.profile && (key == '!')) {
		new Profwnd(findchild(SlenHud.class), findchild(MapView.class).mask.prof, "ILM prof");
	    } else if(key+96 == 'o' && ev.isControlDown())
	    {
	   	    if(opts == null)
	   	    {
	   	    	opts = new GameOptions(this);
	   	    	ui.bind(opts, CustomConfig.wdgtID++);
	   	    }
	    	else opts.toggle();
	    } else if(key != 0) {
		wdgmsg("gk", (int)key);
	    }
	}
	return(true);
    }

    public void draw(GOut g) {
	super.draw(g);
	if(!afk && (System.currentTimeMillis() - ui.lastevent > 300000)) {
	    afk = true;
	    Widget slen = findchild(SlenHud.class);
	    if(slen != null)
		slen.wdgmsg("afk");
	} else if(afk && (System.currentTimeMillis() - ui.lastevent < 300000)) {
	    afk = false;
	}
    }
}
