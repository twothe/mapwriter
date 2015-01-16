package mapwriter.gui;

import mapwriter.Mw;
import mapwriter.forge.MwKeyHandler;
import mapwriter.map.Marker;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mapwriter.Config;
import mapwriter.forge.MapWriter;

@SideOnly(Side.CLIENT)
public class MwGui extends GuiScreen {

  protected final LargeMap mapDisplay;

  private final static double PAN_FACTOR = 0.3D;

  private static final int menuY = 5;
  private static final int menuX = 5;

  private int mouseLeftHeld = 0;
  //private int mouseRightHeld = 0;
  //private int mouseMiddleHeld = 0;
  private int mouseLeftDragStartX = 0;
  private int mouseLeftDragStartY = 0;
  private double viewXCenter;
  private double viewZCenter;
  private Marker movingMarker = null;
  private int movingMarkerXStart = 0;
  private int movingMarkerZStart = 0;
  private int mouseBlockX = 0;
  private int mouseBlockY = 0;
  private int mouseBlockZ = 0;

  private int exit = 0;

  private final Label helpLabel;
  private final Label optionsLabel;
  private final Label dimensionLabel;
  private final Label groupLabel;
  private final Label overlayLabel;

  class Label {

    int x = 0, y = 0, w = 1, h = 12;

    public Label() {
    }

    public void draw(int x, int y, String s) {
      this.x = x;
      this.y = y;
      this.w = MwGui.this.fontRendererObj.getStringWidth(s) + 4;
      MwGui.drawRect(this.x, this.y, this.x + this.w, this.y + this.h, 0x80000000);
      MwGui.this.drawString(MwGui.this.fontRendererObj, s, this.x + 2, this.y + 2, 0xffffff);
    }

    public void drawToRightOf(Label label, String s) {
      this.draw(label.x + label.w + 5, label.y, s);
    }

    public boolean posWithin(int x, int y) {
      return (x >= this.x) && (y >= this.y) && (x <= (this.x + this.w)) && (y <= (this.y + this.h));
    }
  }

  public MwGui() {
    this.mapDisplay = new LargeMap();
    this.mapDisplay.centerMapOnPlayer();

    this.helpLabel = new Label();
    this.optionsLabel = new Label();
    this.dimensionLabel = new Label();
    this.groupLabel = new Label();
    this.overlayLabel = new Label();
  }

  // called when gui is displayed and every time the screen
  // is resized
  @Override
  public void initGui() {
    MapWriter.log.info("initGui");
  }

  // called when a button is pressed
  @Override
  protected void actionPerformed(GuiButton button) {
    MapWriter.log.info("actionPerformed");
  }

  public void exitGui() {
    Keyboard.enableRepeatEvents(false);
    this.mc.displayGuiScreen((GuiScreen) null);
    this.mc.setIngameFocus();
    this.mc.getSoundHandler().resumeSounds();
  }

  // get a marker near the specified block pos if it exists.
  // the maxDistance is based on the view width so that you need to click closer
  // to a marker when zoomed in to select it.
  public Marker getMarkerNearScreenPos(int x, int y) {
    Marker nearMarker = null;
    for (Marker marker : Mw.instance.markerManager.visibleMarkerList) {
      if (marker.screenPos != null) {
        if (marker.screenPos.distanceSq(x, y) < 6.0) {
          nearMarker = marker;
        }
      }
    }
    return nearMarker;
  }

  public int getHeightAtBlockPos(int bX, int bZ) {
    int bY = 0;
    int worldDimension = Mw.instance.mc.theWorld.provider.dimensionId;
    if ((worldDimension == this.mapDisplay.getDimensionID()) && (worldDimension != -1)) {
      bY = Mw.instance.mc.theWorld.getChunkFromBlockCoords(bX, bZ).getHeightValue(bX & 0xf, bZ & 0xf);
    }
    return bY;
  }

  public boolean isPlayerNearScreenPos(int x, int y) {
//    Point.Double p = this.map.playerArrowScreenPos;
//    return p.distanceSq(x, y) < 9.0;
    return false;
  }

  public void deleteSelectedMarker() {
    if (Mw.instance.markerManager.selectedMarker != null) {
      //MwUtil.log("deleting marker %s", Mw.instance.markerManager.selectedMarker.name);
      Mw.instance.markerManager.delMarker(Mw.instance.markerManager.selectedMarker);
      Mw.instance.markerManager.update();
      Mw.instance.markerManager.selectedMarker = null;
    }
  }

  // c is the ascii equivalent of the key typed.
  // key is the lwjgl key code.
  @Override
  protected void keyTyped(char c, int key) {
    //MwUtil.log("MwGui.keyTyped(%c, %d)", c, key);
    switch (key) {
      case Keyboard.KEY_ESCAPE:
        this.exitGui();
        break;

      case Keyboard.KEY_DELETE:
        this.deleteSelectedMarker();
        break;

      case Keyboard.KEY_SPACE:
        // next marker group
        Mw.instance.markerManager.nextGroup();
        Mw.instance.markerManager.update();
        break;

      case Keyboard.KEY_C:
        // cycle selected marker colour
        if (Mw.instance.markerManager.selectedMarker != null) {
          Mw.instance.markerManager.selectedMarker.colourNext();
        }
        break;

      case Keyboard.KEY_N:
        // select next visible marker
        Mw.instance.markerManager.selectNextMarker();
        break;

      case Keyboard.KEY_HOME:
        // centre map on player
        this.mapDisplay.centerMapOnPlayer();
        break;

      case Keyboard.KEY_T:
        if (Mw.instance.markerManager.selectedMarker != null) {
          Mw.instance.teleportToMarker(Mw.instance.markerManager.selectedMarker);
          this.exitGui();
        } else {
          //TODO: Notify player that teleport is disabled
        }
        break;

      default:
        if (key == MwKeyHandler.keyMapGui.getKeyCode()) {
          // exit on the next tick
          this.exit = 1;
        } else if (key == MwKeyHandler.keyNextGroup.getKeyCode()) {
          Mw.instance.markerManager.nextGroup();
          Mw.instance.markerManager.update();
        }
        break;
    }
  }

  // override GuiScreen's handleMouseInput to process
  // the scroll wheel.
  @Override
  public void handleMouseInput() {
    int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
    int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
    int direction = Mouse.getEventDWheel();
    if (direction != 0) {
      this.mouseDWheelScrolled(x, y, direction);
    }
    super.handleMouseInput();
  }

  // mouse button clicked. 0 = LMB, 1 = RMB, 2 = MMB
  @Override
  protected void mouseClicked(int x, int y, int button) {
    	//MwUtil.log("MwGui.mouseClicked(%d, %d, %d)", x, y, button);

    //int bX = this.mouseToBlockX(x);
    //int bZ = this.mouseToBlockZ(y);
    //int bY = this.getHeightAtBlockPos(bX, bZ);
    Marker marker = this.getMarkerNearScreenPos(x, y);
    Marker prevMarker = Mw.instance.markerManager.selectedMarker;

    if (button == 0) {
      if (this.optionsLabel.posWithin(x, y)) {
        this.mc.displayGuiScreen(new MwGuiOptions(this));
      } else {
        this.mouseLeftHeld = 1;
        this.mouseLeftDragStartX = x;
        this.mouseLeftDragStartY = y;
        Mw.instance.markerManager.selectedMarker = marker;

        if ((marker != null) && (prevMarker == marker)) {
          // clicked previously selected marker.
          // start moving the marker.
          this.movingMarker = marker;
          this.movingMarkerXStart = marker.x;
          this.movingMarkerZStart = marker.z;
        }
      }

    } else if (button == 1) {
      //this.mouseRightHeld = 1;
      if ((marker != null) && (prevMarker == marker)) {
        // right clicked previously selected marker.
        // edit the marker
        this.mc.displayGuiScreen(
                new MwGuiMarkerDialog(
                        this,
                        Mw.instance.markerManager,
                        marker
                )
        );

      } else if (marker == null) {
        // open new marker dialog
        String group = Mw.instance.markerManager.getVisibleGroupName();
        if (group.equals("none")) {
          group = "group";
        }

        int mx, my, mz;
        if (this.isPlayerNearScreenPos(x, y)) {
          // marker at player's locations
          mx = Mw.instance.player.xInt;
          my = Mw.instance.player.yInt;
          mz = Mw.instance.player.zInt;

        } else {
          // marker at mouse pointer location
          mx = this.mouseBlockX;
          my = (this.mouseBlockY > 0) ? this.mouseBlockY : Config.instance.defaultTeleportHeight;
          mz = this.mouseBlockZ;
        }
        this.mc.displayGuiScreen(
                new MwGuiMarkerDialog(
                        this,
                        Mw.instance.markerManager,
                        "",
                        group,
                        mx, my, mz,
                        this.mapDisplay.getDimensionID()
                )
        );
      }
    } else if (button == 2) {
    }

    this.mapDisplay.centerMapOn(PAN_FACTOR, PAN_FACTOR);
    this.viewXCenter = this.mapDisplay.getMapCenterX();
    this.viewZCenter = this.mapDisplay.getMapCenterZ();
    //this.viewSizeStart = this.mapManager.getViewSize();
  }

  // mouse button released. 0 = LMB, 1 = RMB, 2 = MMB
  // not called on mouse movement.
  @Override
  protected void mouseMovedOrUp(int x, int y, int button) {
    //MwUtil.log("MwGui.mouseMovedOrUp(%d, %d, %d)", x, y, button);
    if (button == 0) {
      this.mouseLeftHeld = 0;
      this.movingMarker = null;
    } else if (button == 1) {
      //this.mouseRightHeld = 0;
    }
  }

  // zoom on mouse direction wheel scroll
  public void mouseDWheelScrolled(int x, int y, int direction) {
    Marker marker = this.getMarkerNearScreenPos(x, y);
    if ((marker != null) && (marker == Mw.instance.markerManager.selectedMarker)) {
      if (direction > 0) {
        marker.colourNext();
      } else {
        marker.colourPrev();
      }

    } else if (this.dimensionLabel.posWithin(x, y)) {
      int n = (direction > 0) ? 1 : -1;
      this.mapDisplay.setDimensionID(Mw.instance.nextDimension(n));

    } else if (this.groupLabel.posWithin(x, y)) {
      int n = (direction > 0) ? 1 : -1;
      Mw.instance.markerManager.nextGroup(n);
      Mw.instance.markerManager.update();
    } else if (this.overlayLabel.posWithin(x, y)) {

    } else {
      this.mapDisplay.modifyZoomLevel((direction > 0) ? -1 : 1);
    }
  }

  // called every frame
  @Override
  public void updateScreen() {
    //MwUtil.log("MwGui.updateScreen() " + Thread.currentThread().getName());
    // need to wait one tick before exiting so that the game doesn't
    // handle the 'm' key and re-open the gui.
    // there should be a better way.
    if (this.exit > 0) {
      this.exit++;
    }
    if (this.exit > 2) {
      this.exitGui();
    }
    super.updateScreen();
  }

  public void drawStatus(int bX, int bY, int bZ) {
    String s;
    if (bY != 0) {
      s = String.format("cursor: (%d, %d, %d)", bX, bY, bZ);
    } else {
      s = String.format("cursor: (%d, ?, %d)", bX, bZ);
    }
    if (this.mc.theWorld != null) {
      if (!this.mc.theWorld.getChunkFromBlockCoords(bX, bZ).isEmpty()) {
        s += String.format(", biome: %s", this.mc.theWorld.getBiomeGenForCoords(bX, bZ).biomeName);
      }
    }
    drawRect(10, this.height - 21, this.width - 20, this.height - 6, 0x80000000);
    this.drawCenteredString(this.fontRendererObj,
            s, this.width / 2, this.height - 18, 0xffffff);
  }

  public void drawHelp() {
    drawRect(10, 20, this.width - 20, this.height - 30, 0x80000000);
    this.fontRendererObj.drawSplitString(
            "Keys:\n\n"
            + "  Space\n"
            + "  Delete\n"
            + "  C\n"
            + "  Home\n"
            + "  End\n"
            + "  N\n"
            + "  T\n"
            + "  P\n"
            + "  R\n"
            + "  U\n\n"
            + "Left click drag or arrow keys pan the map.\n"
            + "Mouse wheel or Page Up/Down zooms map.\n"
            + "Right click map to create a new marker.\n"
            + "Left click drag a selected marker to move it.\n"
            + "Mouse wheel over selected marker to cycle colour.\n"
            + "Mouse wheel over dimension or group box to cycle.\n",
            15, 24, this.width - 30, 0xffffff);
    this.fontRendererObj.drawSplitString(
            "| Next marker group\n"
            + "| Delete selected marker\n"
            + "| Cycle selected marker colour\n"
            + "| Centre map on player\n"
            + "| Centre map on selected marker\n"
            + "| Select next marker\n"
            + "| Teleport to cursor or selected marker\n"
            + "| Save PNG of visible map area\n"
            + "| Regenerate visible map area from region files\n"
            + "| Underground map mode\n",
            75, 42, this.width - 90, 0xffffff);
  }

  public void drawMouseOverHint(int x, int y, String title, int mX, int mY, int mZ) {
    String desc = String.format("(%d, %d, %d)", mX, mY, mZ);
    int stringW = Math.max(
            this.fontRendererObj.getStringWidth(title),
            this.fontRendererObj.getStringWidth(desc));

    x = Math.min(x, this.width - (stringW + 16));
    y = Math.min(Math.max(10, y), this.height - 14);

    drawRect(x + 8, y - 10, x + stringW + 16, y + 14, 0x80000000);
    this.drawString(this.fontRendererObj,
            title,
            x + 10, y - 8, 0xffffff);
    this.drawString(this.fontRendererObj,
            desc,
            x + 10, y + 4, 0xcccccc);
  }

  // also called every frame
  @Override
  public void drawScreen(int mouseX, int mouseY, float f) {

    this.drawDefaultBackground();
    //double zoomFactor = 1.0;

    if (this.mouseLeftHeld > 2) {
      final double xOffset = (this.mouseLeftDragStartX - mouseX);
      final double yOffset = (this.mouseLeftDragStartY - mouseY);

      if (this.movingMarker != null) {
        double scale = 1.0;
        this.movingMarker.x = this.movingMarkerXStart - (int) (xOffset / scale);
        this.movingMarker.z = this.movingMarkerZStart - (int) (yOffset / scale);
      } else {
        this.mapDisplay.centerMapOn(this.viewXCenter + xOffset, this.viewZCenter + yOffset);
      }
    }

    if (this.mouseLeftHeld > 0) {
      this.mouseLeftHeld++;
    }

    // draw the map
    this.mapDisplay.setSize(this.width, this.height, 4);
    this.mapDisplay.setCenter(this.width / 2, this.height / 2);
    this.mapDisplay.draw();

    // let the renderEngine know we have changed the texture.
    //this.mc.renderEngine.resetBoundTexture();
    // get the block the mouse is currently hovering over
    this.mouseBlockX = this.mapDisplay.getBlockCoordinateX(mouseX);
    this.mouseBlockZ = this.mapDisplay.getBlockCoordinateY(mouseY);
    this.mouseBlockY = this.getHeightAtBlockPos(this.mouseBlockX, this.mouseBlockZ);

    // draw name of marker under mouse cursor
    Marker marker = this.getMarkerNearScreenPos(mouseX, mouseY);
    if (marker != null) {
      this.drawMouseOverHint(mouseX, mouseY, marker.name, marker.x, marker.y, marker.z);
    }

    // draw name of player under mouse cursor
    if (this.isPlayerNearScreenPos(mouseX, mouseY)) {
      this.drawMouseOverHint(mouseX, mouseY, this.mc.thePlayer.getDisplayName(),
              Mw.instance.player.xInt,
              Mw.instance.player.yInt,
              Mw.instance.player.zInt);
    }

    // draw status message
    this.drawStatus(this.mouseBlockX, this.mouseBlockY, this.mouseBlockZ);

    // draw labels
    this.helpLabel.draw(menuX, menuY, "[help]");
    this.optionsLabel.drawToRightOf(this.helpLabel, "[options]");
    String dimString = String.format("[dimension: %d]", this.mapDisplay.getDimensionID());
    this.dimensionLabel.drawToRightOf(this.optionsLabel, dimString);
    String groupString = String.format("[group: %s]", Mw.instance.markerManager.getVisibleGroupName());
    this.groupLabel.drawToRightOf(this.dimensionLabel, groupString);
    String overlayString = String.format("[overlay : %s]", "none");
    this.overlayLabel.drawToRightOf(this.groupLabel, overlayString);

    // help message on mouse over
    if (this.helpLabel.posWithin(mouseX, mouseY)) {
      this.drawHelp();
    }

    super.drawScreen(mouseX, mouseY, f);
  }
}
