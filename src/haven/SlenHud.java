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

import java.util.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import org.relayirc.util.*;
import org.relayirc.core.*;
import org.relayirc.chatengine.*;
import static haven.Inventory.invsq;

public class SlenHud extends Widget implements DTarget, DropTarget {
	public static final int _BELTSIZE = 10;
    public static final Tex bg = Resource.loadtex("gfx/hud/slen/low");
    public static final Tex flarps = Resource.loadtex("gfx/hud/slen/flarps");
    public static final Tex mbg = Resource.loadtex("gfx/hud/slen/mcircle");
    public static final Tex dispbg = Resource.loadtex("gfx/hud/slen/dispbg");
    public static final Coord fc = new Coord(96, -29);
    public static final Coord mc = new Coord(316, -55);
    public static final Coord dispc = new Coord(0, 4 - dispbg.sz().y);
    public static final Coord bc1 = new Coord(147, -8);	//	Belt 1 location start
    public static final Coord bc2 = new Coord(485, -8); //	Belt 2 location start
    public static final Coord sz;
    public static int activeBelt = 0;
    public List<SlenChat> ircChannels = new ArrayList<SlenChat>();
    public SlenConsole ircConsole;
    int woff = 0;
    List<HWindow> wnds = new ArrayList<HWindow>();
    HWindow awnd;
    Map<HWindow, Button> btns = new HashMap<HWindow, Button>();
    IButton hb, invb, equb, chrb, budb;
    Button sub, sdb;
    VC vc;
    String cmdline = null;
    static Text.Foundry errfoundry = new Text.Foundry(new java.awt.Font("SansSerif",
    												java.awt.Font.BOLD, 14), new Color(192, 0, 0));
    static Text.Foundry cmdfoundry = new Text.Foundry(new java.awt.Font("Monospaced",
    												java.awt.Font.PLAIN, 12), new Color(245, 222, 179));
    Text cmdtext, lasterr;
    long errtime;
    @SuppressWarnings("unchecked")
    Resource[][] belt = new Resource[_BELTSIZE][_BELTSIZE];

    static {
	Widget.addtype("slen", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new SlenHud(c, parent));
		}
	    });
	int h = bg.sz().y;
	sz = new Coord(CustomConfig.windowSize.x, h);
	sz.y = (h - fc.y > sz.y)?(h - fc.y):sz.y;
	sz.y = (h - mc.y > sz.y)?(h - mc.y):sz.y;
    }

    static class VC {
	static final long ms = 500;
	SlenHud m;
	IButton sb;
	long st;
	boolean w, c;

	VC(SlenHud m, IButton sb) {
	    this.m = m;
	    this.sb = sb;
	    w = c = true;
	}

	void hide() {
	    st = System.currentTimeMillis();
	    w = false;
	}

	void show() {
	    st = System.currentTimeMillis();
	    w = true;
	}

	void toggle() {
	    st = System.currentTimeMillis();
	    w = !w;
	    c = !w;
	}

	void tick() {
	    long ct = System.currentTimeMillis() - st;
	    double ca;
	    if(ct >= ms) {
		ca = 1;
	    } else {
		ca = (double)ct / (double)ms;
	    }
	    if(!w && c) {
		if(ca < 0.6) {
		    m.c.y = CustomConfig.windowSize.y - (int)(sz.y * (1 - (ca / 0.6)));
		} else {
		    m.c.y = CustomConfig.windowSize.y;
		    sb.c.y = CustomConfig.windowSize.y - (int)(sb.sz.y * ((ca - 0.6) / 0.4));
		}
	    }
	    if(w && !c) {
		if(ca < 0.6) {
		    m.c.y = CustomConfig.windowSize.y - (int)(sz.y * (ca / 0.6));
		    sb.c.y = CustomConfig.windowSize.y - (int)(sb.sz.y * (1 - (ca / 0.6)));
		} else {
		    m.c.y = CustomConfig.windowSize.y - sz.y;
		    sb.c.y = CustomConfig.windowSize.y;
		}
	    }
	    if(ct >= ms)
		c = w;
	}
    }

    public SlenHud(Coord c, Widget parent) {
	super(new Coord(CustomConfig.windowSize.x, CustomConfig.windowSize.y).add(sz.inv()), sz, parent);
	new Img(fc, flarps, this);
	new Img(mc, mbg, this);
	new Img(dispc, dispbg, this);

	//	Hide button
	hb = new IButton(mc, this, Resource.loadimg("gfx/hud/slen/hbu"), Resource.loadimg("gfx/hud/slen/hbd"));

	//	Inventory button
	invb = new IButton(mc, this, Resource.loadimg("gfx/hud/slen/invu"), Resource.loadimg("gfx/hud/slen/invd"));

	//	Equipment button
	equb = new IButton(mc, this, Resource.loadimg("gfx/hud/slen/equu"), Resource.loadimg("gfx/hud/slen/equd"));

	//	Character button
	chrb = new IButton(mc, this, Resource.loadimg("gfx/hud/slen/chru"), Resource.loadimg("gfx/hud/slen/chrd"));

	//	Kin list button
	budb = new IButton(mc, this, Resource.loadimg("gfx/hud/slen/budu"), Resource.loadimg("gfx/hud/slen/budd"));
	{
		//	Village claims button
	    new IButton(dispc, this, Resource.loadimg("gfx/hud/slen/dispauth"),
	    			 Resource.loadimg("gfx/hud/slen/dispauthd")) {
		public void click() {
		    MapView mv = ui.root.findchild(MapView.class);
		    mv.authdraw = !mv.authdraw;
		    Utils.setpref("authdraw", mv.authdraw?"on":"off");
		}
	    };
	}
	{
		//	Totem claim button
	    new IButton(dispc, this, Resource.loadimg("gfx/hud/slen/dispclaim"),
	    				 Resource.loadimg("gfx/hud/slen/dispclaimd")) {
		private boolean v = false;

		public void click() {
		    MapView mv = ui.root.findchild(MapView.class);
		    if(v) {
			mv.disol(0, 1);
			v = false;
		    } else {
			mv.enol(0, 1);
			v = true;
		    }
		}
	    };
	}
	vc = new VC(this, new IButton(new Coord(492, CustomConfig.windowSize.y), parent,
				 Resource.loadimg("gfx/hud/slen/sbu"), Resource.loadimg("gfx/hud/slen/sbd")) {
		public void click() {
		    vc.show();
		}
	    });
	sub = new Button(new Coord(134, 29), 100, this, Resource.loadimg("gfx/hud/slen/sau")) {
		public void click() {
		    sup();
		}
	    };
	sdb = new Button(new Coord(134, 109), 100, this, Resource.loadimg("gfx/hud/slen/sad")) {
		public void click() {
		    sdn();
		}
	    };
	new MiniMap(new Coord(5, 5), new Coord(125, 125), this, ui.mainview);
	sub.visible = sdb.visible = false;

	//	Load the current belt
	initBelt();

	//	Global Chat
	ircConsole = new SlenConsole(this);
	ui.bind(ircConsole, CustomConfig.wdgtID++);
    }

    public Coord xlate(Coord c, boolean in) {
	Coord bgc = sz.add(bg.sz().inv());
	if(in)
	    return(c.add(bgc));
	else
	    return(c.add(bgc.inv()));
    }


	/*
	 *	CLIENT COMMAND PARSER
	 */
    public void runcmd(String[] argv) {
	String cmd = argv[0].intern();
	boolean die = false;
	try {
	    if(cmd == "q") {
		Utils.tg().interrupt();
	    } else if(cmd == "lo") {
	    	Coord center = new Coord((CustomConfig.windowSize.x-125)/2, (CustomConfig.windowSize.y-50)/2);
	    	new Logout(center, parent);
		//ui.sess.close();
	    } else if(cmd == "afk") {
		wdgmsg("afk");
	    } else if(cmd == "fs") {
		if((argv.length >= 2) && (ui.fsm != null)) {
		    if(Utils.atoi(argv[1]) != 0)
			ui.fsm.setfs();
		    else
			ui.fsm.setwnd();
		}
	    } else if(cmd == "sfx") {
		Audio.play(Resource.load(argv[1]));
	    } else if(cmd == "bgm") {
		int i = 1;
		String opt;
		boolean loop = false;
		if(i < argv.length) {
		    while((opt = argv[i]).charAt(0) == '-') {
			i++;
			if(opt.equals("-l"))
			    loop = true;
		    }
		    String resnm = argv[i++];
		    int ver = -1;
		    if(i < argv.length)
			ver = Integer.parseInt(argv[i++]);
		    Music.play(Resource.load(resnm, ver), loop);
		} else {
		    Music.play(null, false);
		}
	    } else if(cmd == "texdis") {
		TexGL.disableall = (Integer.parseInt(argv[1]) != 0);
	    } else if(cmd == "die") {
		die = true;
	    } else if(cmd == "browse") {
		if(WebBrowser.self != null) {
		    WebBrowser.self.show(new java.net.URL(argv[1]));
		} else {
		    error("No web browser available");
		}
	    } else if(cmd == "threads") {
		java.io.StringWriter out = new java.io.StringWriter();
		Utils.dumptg(null, new java.io.PrintWriter(out));
		for(HWindow w : wnds) {
		    if(w.title.equals("Messages")) {
			for(String line : Utils.splitlines(out.toString()))
			    ((Logwindow)w).log.append(line);
		    }
		}
	    } else if(cmd == "cam") {
		if(argv.length >= 2) {
		    MapView mv = ui.root.findchild(MapView.class);
		    if(argv[1].equals("orig")) {
			mv.cam = new MapView.OrigCam();
		    } else if(argv[1].equals("kingsquest")) {
			mv.cam = new MapView.WrapCam();
		    } else if(argv[1].equals("border")) {
			mv.cam = new MapView.BorderCam();
		    } else if(argv[1].equals("predict")) {
			mv.cam = new MapView.PredictCam();
		    } else if(argv[1].equals("fixed")) {
			mv.cam = new MapView.FixedCam();
		    }
		}
	    } else if(cmd == "plol") {
		MapView mv = ui.root.findchild(MapView.class);
		Indir<Resource> res = Resource.load(argv[1]).indir();
		Message sdt;
		if(argv.length > 2)
		    sdt = new Message(0, Utils.hex2byte(argv[2]));
		else
		    sdt = new Message(0);
		Gob pl;
		if((mv.playergob >= 0) && ((pl = ui.sess.glob.oc.getgob(mv.playergob)) != null))
		    pl.ols.add(new Gob.Overlay(-1, res, sdt));
	    } else if(cmd == "sfxvol") {
		Audio.setvolume(Double.parseDouble(argv[1]));
	    } else {
		error(cmd + ": no such command");
	    }
	} catch(Exception e) {
	    error(e.getMessage());
	}
	if(die)
	    throw(new RuntimeException("Triggered death"));
    }

    public void error(String err) {
	lasterr = errfoundry.render(err);
	errtime = System.currentTimeMillis();
    }

    private Coord beltc(int i) {
	if(i < 5) {
	    return(bc1.add(i * (invsq.sz().x + 2), 0));
	} else {
	    return(bc2.add((i - 5) * (invsq.sz().x + 2), 0));
	}
    }

    private int beltslot(Coord c) {
	c = xlate(c, false);
	int sw = invsq.sz().x + 2;
	if((c.x >= bc1.x) && (c.y >= bc1.y) && (c.y < bc1.y + invsq.sz().y)) {
	    if((c.x - bc1.x) / sw < 5) {
		if((c.x - bc1.x) % sw < invsq.sz().x)
		    return((c.x - bc1.x) / sw);
	    }
	}
	if((c.x >= bc2.x) && (c.y >= bc2.y) && (c.y < bc2.y + invsq.sz().y)) {
	    if((c.x - bc2.x) / sw < 5) {
		if((c.x - bc2.x) % sw < invsq.sz().x)
		    return(((c.x - bc2.x) / sw) + 5);
	    }
	}
	return(-1);
    }

    public void draw(GOut g) {
	vc.tick();
	if(!ui.sess.alive())
	{
		ircConsole.IRC.close();
		for(SlenChat tSCWnd : ircChannels)
		{
			remwnd(tSCWnd);
		}
		ircChannels.clear();
	}
	Coord bgc = sz.add(bg.sz().inv());
	g.image(bg, bgc);
	super.draw(g);

	//	Draws the belt
	for(int i = 0; i < _BELTSIZE; i++) {
	    Coord c = xlate(beltc(i), true);
	    Coord x = c.add(invsq.sz().add(-10,0));
	    g.image(invsq, c);
	    g.chcolor(156, 180, 158, 255);
	    g.atext(Integer.toString((i + 1) % 10), c.add(invsq.sz()), 1, 1);
	    g.chcolor();
	    Resource res = null;
	    if(belt[activeBelt][i] != null)
			res = belt[activeBelt][i];
	    if(res != null && !res.loading)
			g.image(res.layer(Resource.imgc).tex(), c.add(1, 1));
		g.chcolor(Color.BLACK);
	    g.atext(Integer.toString(activeBelt), x, 1, 1);
	    g.chcolor();
	}

	if(cmdline != null) {
	    GOut eg = g.reclip(new Coord(0, -20), new Coord(sz.x, 20));
	    if((cmdtext == null) || !cmdtext.text.equals(cmdline))
		cmdtext = cmdfoundry.render(":" + cmdline);
	    eg.image(cmdtext.tex(), new Coord(15, 0));
	    eg.line(new Coord(cmdtext.sz().x + 16, 2), new Coord(cmdtext.sz().x + 16, 14), 1);
	} else if(lasterr != null) {
	    if((System.currentTimeMillis() - errtime) > 3000) {
		lasterr = null;
	    } else {
		GOut eg = g.reclip(new Coord(0, -20), new Coord(sz.x, 20));
		eg.image(lasterr.tex(), new Coord(15, 0));
	    }
	}
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == hb) {
	    vc.hide();
	    return;
	} else if(sender == invb) {
	    wdgmsg("inv");
	    return;
	} else if(sender == equb) {
	    wdgmsg("equ");
	    return;
	} else if(sender == chrb) {
	    wdgmsg("chr");
	    return;
	} else if(sender == budb) {
	    wdgmsg("bud");
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "err") {
	    error((String)args[0]);
	} else if(msg == "setbelt") {
		synchronized(belt)
		{
			if(args.length < 2) {
				belt[activeBelt][(Integer)args[0]] = null;
				CustomConfig.activeCharacter.hudBelt[activeBelt][(Integer)args[0]] = null;
		    } else {/*
		    	belt[activeBelt][(Integer)args[0]] = ui.sess.getres((Integer)args[1]).get();
				CustomConfig.activeCharacter.hudBelt[activeBelt][(Integer)args[0]] = belt[activeBelt][(Integer)args[0]].name;
		    */}
		}
	} else {
	    super.uimsg(msg, args);
	}
    }

    private void updbtns() {
	if(wnds.size() <= 5) {
	    woff = 0;
	} else {
	    if(woff < 0)
		woff = 0;
	    if(woff > wnds.size() - 5)
		woff = wnds.size() - 5;
	}
	for(Button b : btns.values())
	    b.visible = false;
	sub.visible = sdb.visible = false;
	for(int i = 0; i < 5; i++) {
	    int wi = i + woff;
	    if(wi >= wnds.size())
		continue;
	    if((i == 0) && (woff > 0)) {
		sub.visible = true;
	    } else if((i == 4) && (woff < wnds.size() - 5)) {
		sdb.visible = true;
	    } else {
		HWindow w = wnds.get(wi);
		Button b = btns.get(w);
		b.visible = true;
		b.c = new Coord(b.c.x, 29 + (i * 20));
	    }
	}
    }

    private void sup() {
	woff--;
	updbtns();
    }

    private void sdn() {
	woff++;
	updbtns();
    }

	//	Handles the switching of windows
    private void setawnd(HWindow wnd) {
    	//	Hide the current active window
    	if(awnd != null && awnd != wnd)
    	{
    		awnd.hide();
    	}
    	//	Some windows have special toggles that act when the button is pressed twice
    	if(awnd == wnd)
		{
			//	IRC SlenChat userlist toggle
			if(awnd.getClass().getName().equalsIgnoreCase(SlenChat.class.getName()))
			{
				if(((SlenChat)awnd).userList != null)	((SlenChat)awnd).userList.toggle();
			}
			return;
		}
		//	Make the specified window be the active window, set the appropriate coloring, and show
		awnd = wnd;
		awnd.hudButton.changeText(awnd.hudButton.text.text, Color.YELLOW);
		awnd.hudButton.isFlashing = false;

		awnd.show();
    }

    public void nextWindow()
    {
    	synchronized(wnds){
    		if(wnds.size() >= 0)
		    	if(wnds.indexOf(awnd)+1 < wnds.size())
		    	{
		    		setawnd(wnds.get(wnds.indexOf(awnd)+1));
		    	} else
		    	{
		    		setawnd(wnds.get(0));
		    	}
    	}
    }

    public void prevWindow()
    {
    	synchronized(wnds){
	    	if(wnds.size() >= 0)
		    	if(wnds.indexOf(awnd)-1 > 0)
		    	{
		    		setawnd(wnds.get(wnds.indexOf(awnd)-1));
		    	} else
		    	{
		    		setawnd(wnds.get(wnds.size()-1));
		    	}
    	}
    }

    public void addwnd(final HWindow wnd) {
	wnds.add(wnd);
	Button wndButton = new Button(new Coord(134, 29), 100, this, wnd.title) {
		public void click() {
			setawnd(wnd);
		}
	};
	wnd.setButton(wndButton);
	btns.put(wnd, wndButton);
	updbtns();
	setawnd(wnd);
    }

    public void remwnd(HWindow wnd) {
	if(wnd == awnd) {
	    int i = wnds.indexOf(wnd);
	    if(wnds.size() == 1)
		setawnd(null);
	    else if(i < 0)
		setawnd(wnds.get(0));
	    else if(i >= wnds.size() - 1)
		setawnd(wnds.get(i - 1));
	    else
		setawnd(wnds.get(i + 1));
	}
	wnds.remove(wnd);
	ui.destroy(btns.get(wnd));
	btns.remove(wnd);
	updbtns();
    }

    public boolean mousedown(Coord c, int button) {
	int slot = beltslot(c);
	if(slot != -1) {
	    wdgmsg("belt", slot, button, ui.modflags());
	    return(true);
	}
	return(super.mousedown(c, button));
    }

    public boolean mousewheel(Coord c, int amount) {
		return awnd.mousewheel(c, amount);
    }

    public boolean globtype(char ch, KeyEvent ev) {

	if(ch == ' ') {
	    vc.toggle();
	    return(true);
	} else if(ch == ':') {
	    ui.grabkeys(this);
	    cmdline = "";
	    return(true);
	} else if((((ch >= '1') && (ch <= '9')) || (ch == '0')) && ev.isAltDown())
	{
		activeBelt = ch-48;
		CustomConfig.activeCharacter.hudActiveBelt = activeBelt;
		for(int i = 0; i < belt[activeBelt].length; i++)
		{
			if(belt[activeBelt][i] == null){
				wdgmsg("setbelt", i, 0);
				continue;
			}

			wdgmsg("setbelt", i, belt[activeBelt][i].name);
		}
		return true;
	} else if(ch == '0') {
	    if(belt[activeBelt][9] != null)
	    	wdgmsg("belt", 9, 1, 0);
	    return(true);
	} else if((ch >= '1') && (ch <= '9')) {
	    if(belt[activeBelt][ch-'1'] != null)
	    	wdgmsg("belt", ch - '1', 1, 0);
	    return(true);
	}
	return(super.globtype(ch, ev));
    }

    public boolean type(char ch, KeyEvent ev) {
	if(cmdline == null) {
	    return(super.type(ch, ev));
	} else {
		//	Commandline text
	    if(ch >= 32) {
		cmdline += ch;
		//	Backspace
	    } else if(ch == 8) {
		if(cmdline.length() > 0) {
		    cmdline = cmdline.substring(0, cmdline.length() - 1);
		} else {
		    cmdline = null;
		    ui.grabkeys(null);
		}
		//	Escape key -- escapes from console
	    } else if(ch == 27) {
		cmdline = null;
		ui.grabkeys(null);
		//	Enter key -- run command
	    } else if(ch == 10) {
		String[] argv = Utils.splitwords(cmdline);
		if(argv != null) {
		    if(argv.length > 0)
			runcmd(argv);
		    cmdline = null;
		    ui.grabkeys(null);
		}
	    }
	    return(true);
	}
    }

    public int foldheight() {
	return(CustomConfig.windowSize.y - c.y);
    }

	public boolean drop(Coord cc, Coord ul) {
		int slot = beltslot(cc);
		if(slot != -1) {
			wdgmsg("setbelt", slot, 0);
			return(true);
		}
		return(false);
	}

	    public boolean iteminteract(Coord cc, Coord ul) {
			return(false);
	    }
    public boolean dropthing(Coord c, Object thing) {
	int slot = beltslot(c);
	if(slot != -1) {
	    if(thing instanceof Resource) {
		Resource res = (Resource)thing;
		if(res.layer(Resource.action) != null) {
			belt[activeBelt][slot] = res;
			CustomConfig.activeCharacter.hudBelt[activeBelt][slot] = belt[activeBelt][slot].name;
		    wdgmsg("setbelt", slot, res.name);
		    return(true);
		}
	    }
	}
	return(false);
    }
    public void initBelt()
    {
    	activeBelt = CustomConfig.activeCharacter.hudActiveBelt;
		synchronized(belt)
		{
			for(int i = 0; i < belt.length; i++)
			{
				for(int j = 0; j < belt[i].length;j++)
				{
					if(CustomConfig.activeCharacter.hudBelt[i][j] != null)
					{
						belt[i][j] = Resource.load(CustomConfig.activeCharacter.hudBelt[i][j]);
					}
				}
			}
		}
    }
    public void destroy()
    {
    	ircConsole.IRC.close();
    	super.destroy();
    }
}

