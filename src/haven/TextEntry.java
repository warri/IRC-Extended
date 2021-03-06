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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.*;
import java.io.IOException;

public class TextEntry extends SSWidget  implements ClipboardOwner{
    public String text;
    public String badchars = "";
    public boolean noNumbers = false;
    public boolean noLetters = false;
    int pos, limit = 0;
    boolean prompt = false, pw = false;
    int cw = 0;

    static {
	Widget.addtype("text", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new TextEntry(c, (Coord)args[0], parent, (String)args[1]));
		}
	    });
    }

    public void settext(String text) {
	this.text = text;
	if(pos > text.length())
	    pos = text.length();
	render();
    }

    public void uimsg(String name, Object... args) {
	if(name == "settext") {
	    settext((String)args[0]);
	} else if(name == "get") {
	    wdgmsg("text", text);
	} else if(name == "limit") {
	    limit = (Integer)args[0];
	} else if(name == "pw") {
	    pw = ((Integer)args[0]) == 1;
	    render();
	} else {
	    super.uimsg(name, args);
	}
    }

    private void render() {
	String dtext;
	if(pw) {		//	Replace the text with stars if its a password
	    dtext = "";
	    for(int i = 0; i < text.length(); i++)
		dtext += "*";
	} else {
	    dtext = text;
	}
	synchronized(ui) {
	    Graphics g = graphics();
	    int tPos = pos;
	    //	Draw the white background and black text
	    g.setColor(Color.WHITE);
	    g.fillRect(0, 0, sz.x, sz.y);
	    g.setColor(Color.BLACK);
	    FontMetrics m = g.getFontMetrics();

	    while(m.getStringBounds(dtext.substring(0, tPos), g).getWidth() > sz.x)
	    {
	    	dtext = dtext.substring(1);
	    	tPos--;
	    }
	    g.drawString(dtext, 0, m.getAscent());

	    //	Draw the vertical line symbolizing the prompt
	    if(hasfocus && prompt) {
		Rectangle2D tm = m.getStringBounds(dtext.substring(0, tPos), g);
		g.drawLine((int)tm.getWidth(), 1, (int)tm.getWidth(), m.getHeight() - 1);
	    }
	    Rectangle2D tm = m.getStringBounds(dtext, g);
	    cw = (int)tm.getWidth();
	    update();
	}
    }

    public void gotfocus() {
	render();
    }

    public void lostfocus() {
	render();
    }

    public TextEntry(Coord c, Coord sz, Widget parent, String deftext) {
	super(c, sz, parent);
	text = deftext;
	pos = text.length();
	render();
	setcanfocus(true);
    }

    public boolean type(char c, KeyEvent ev) {
	try {
	    if(c == 8) {		//	BACKSPACE
			if(pos > 0) {
			    if(pos < text.length())
					text = text.substring(0, pos - 1) + text.substring(pos);
			    else
					text = text.substring(0, pos - 1);
			    pos--;
			}
			return true;
	    }
	    if(c == 10) {	//	ENTER
			if(!canactivate)
			    return(false);
			wdgmsg("activate", text);
			return true;
	    }
	    if(c == 127) {	//	DELETE
			if(pos < text.length())
			{
				text = text.substring(0, pos) + text.substring(pos + 1);
			}
			return true;
	    }
	    if(c+96 == 'v' && ev.isControlDown()) {
	    	String clipboardContents = getClipboardContents();
	    	text = text.substring(0, pos) + clipboardContents + text.substring(pos);
	    	pos += clipboardContents.length();
	    	return true;
	    }

	    	if(Character.isDigit(c) && noNumbers && !ev.isAltDown() || badchars.indexOf(c) > -1)
	        {
	            ev.consume();
	            return true;
	        }
	        if(Character.isLetter(c) && noLetters && !ev.isAltDown() || badchars.indexOf(c) > -1)
	        {
	        	ev.consume();
	        	return true;
	        }
	    if(c >= 32) {
			String nt = text.substring(0, pos) + c + text.substring(pos);
			if((limit == 0) || ((limit > 0) && (nt.length() <= limit)) || ((limit == -1) && (cw < sz.x))) {
			    text = nt;
			    pos++;
			}
			return(true);
	    }
	} finally {
	    render();
	}
	return(false);
    }

    public boolean keydown(KeyEvent e) {
	if(e.getKeyCode() == KeyEvent.VK_LEFT) {
	    if(pos > 0)
		pos--;
	} else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
	    if(pos < text.length())
		pos++;
	} else if(e.getKeyCode() == KeyEvent.VK_HOME) {
	    pos = 0;
	} else if(e.getKeyCode() == KeyEvent.VK_END) {
	    pos = text.length();
	}
	render();
	return(true);
    }

    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	render();
	return(true);
    }

    public void draw(GOut g) {
	boolean prompt = System.currentTimeMillis() % 1000 > 500;
	if(prompt != this.prompt) {
	    this.prompt = prompt;
	    render();
	}
	super.draw(g);
    }

	/**
	 * Method lostOwnership
	 *
	 *
	 * @param clipboard
	 * @param contents
	 *
	 */
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// TODO: Add your code here
	}
	public String getClipboardContents()
	{
    	String result = "";
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    //odd: the Object param of getContents is not currently used
	    Transferable contents = clipboard.getContents(null);
	    boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (hasTransferableText)
		{
			try
			{
		 		result = (String)contents.getTransferData(DataFlavor.stringFlavor);
			}
			catch(UnsupportedFlavorException ufe)
			{
				ufe.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
	    }
		return result;
	}
}
