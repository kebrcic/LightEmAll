import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.HashMap;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.Comparator;

//Utils class
class Utils {
  Utils() {}

  //creates an array list of tiles, essentially a sample board with tiles.
  ArrayList<ArrayList<GamePiece>> createBoard(int r, int c, int prow, int pcol) {
    ArrayList<GamePiece> col = new ArrayList<GamePiece>();
    ArrayList<ArrayList<GamePiece>> result = new ArrayList<ArrayList<GamePiece>>();
    Utils util = new Utils();
    for (int i = 0; i < c; i = i + 1) {
      for (int j = 0; j < r; j = j + 1) {
        if (i == pcol && j == prow) {
          col.add(new GamePiece(j, i, true, true, true, true, true));
        }
        else {
          col.add(new GamePiece(j, i, true, true, true, true, false));
        }
      }
      result.add(col);
      col = new ArrayList<GamePiece>();
    }
    return result;
  }

  //creates the ArrayList of Nodes which are tiles.
  ArrayList<GamePiece> createNodes(ArrayList<ArrayList<GamePiece>> board) {
    ArrayList<GamePiece> nodes = new ArrayList<GamePiece>();
    for (ArrayList<GamePiece> columns : board) {
      for (GamePiece tile : columns) {
        nodes.add(tile);     
      }     
    }
    return nodes;
  }

  //finds the original node in the given HashMap 
  GamePiece find(HashMap<GamePiece, GamePiece> hm, GamePiece gp) {
    GamePiece prev = gp;
    GamePiece next = hm.get(prev);
    while (!prev.equals(next)) {
      next = hm.get(next);
      prev = hm.get(prev);
    }
    return next;
  }
}

//compares two edges
class compareEdges implements Comparator<Edge> {
  compareEdges() {}

  //compares two edges by their weights
  public int compare(Edge o1, Edge o2) {
    return o1.weight - o2.weight;
  }
}

//represents an edge connecting two tiles together
class Edge {
  GamePiece start; //where the edge is at
  GamePiece end; //the tile which it connects to
  int weight; //weight of the edge

  Edge(GamePiece start, GamePiece end, int weight) {
    this.start = start;
    this.end = end;
    this.weight = weight;
  }
}

//represents a GamePiece (a cell on the board)
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;
  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;
  //represents the neighbors of a GamePiece
  ArrayList<GamePiece> neighbors;
  //out edges
  ArrayList<Edge> outEdges;
  //represents the relative distance from this gp to the powerStation 
  Double relDist;

  GamePiece(int row, int col, boolean left, boolean right, 
      boolean top, boolean bottom, boolean powerStation) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = false;   
    this.neighbors = new ArrayList<GamePiece>();
    this.outEdges = new ArrayList<Edge>();
    this.relDist = 1.1;
  }

  //Effect: rotates the GamePiece by 90 degrees clockwise.
  //mutates the fields: left, top, right, bottom.
  void rotate() {
    boolean temp = left;
    left = top;
    top = right;
    right = bottom;
    bottom = temp;
  }


  //Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the power station
  //
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that can't be)
    if ((this.relDist <= 0.333) && (this.powered)) {
      wireColor = Color.YELLOW;
    }
    if ((this.relDist > 0.333) && (this.relDist <= 0.666) && (this.powered)) {
      wireColor = Color.YELLOW.darker();
    }
    if ((this.relDist > 0.666) && (this.relDist <= 1) && (this.powered)) {
      wireColor = Color.ORANGE.darker();
    }

    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) { 
      image = 
          new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image); }
    if (this.right) { 
      image = 
          new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image); }
    if (this.bottom) { 
      image = 
          new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image); }
    if (this.left) { 
      image = 
          new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image); }
    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(
              new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    return image;
  }
}

//represents the Game class
class LightEmAll extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;
  // a list of all nodes
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree
  ArrayList<Edge> mst;
  // the width and height of the board
  int width;
  int height;
  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;
  //for testing
  Random ran;
  int sec; //represents the seconds
  int minutes; //represents the minutes

  int score; //represents how many steps the player takes before connecting all the wires

  LightEmAll(int width, int height) {
    this.width = width;
    this.height = height;
    this.radius = (int) Math.ceil(this.height / 2);
    this.sec = 0;
    this.minutes = 0;
    this.mst = new ArrayList<Edge>();
    this.powerCol = 0;
    this.powerRow = 0;
    this.board = new Utils().createBoard(width, height, this.powerRow, this.powerCol); 
    this.nodes = new Utils().createNodes(this.board);
    this.initMst();
    this.mst = generateTree();
    this.calcWires();
    this.score = 0;
    this.addConnectedNeighbors();
    this.updatePower();
  }

  //For Testing
  LightEmAll(int width, int height, Random ran) {
    this.width = width;
    this.height = height;
    this.radius = (int) Math.ceil(this.height / 2) + 1;
    this.sec = 0;
    this.minutes = 0;
    this.mst = new ArrayList<Edge>();
    this.powerCol = 0;
    this.powerRow = 0;
    this.board = new Utils().createBoard(width, height, this.powerRow, this.powerCol); 
    this.nodes = new Utils().createNodes(this.board); 
    this.ran = ran;
    this.testInitMst();
    this.mst = generateTree();
    this.calcWires();
    this.score = 0;
    this.addConnectedNeighbors();
    this.updatePower();
  }


  @Override
  //renders the board. Also checks if the user has won
  //to display the winning WorldScene.
  public WorldScene makeScene() {
    WorldScene win = new WorldScene(this.width * 30, this.height * 30);
    WorldScene w = new WorldScene(this.board.size() * 30, this.board.get(0).size() * 30);
    if (this.allPowered(board)) {
      win.placeImageXY(new TextImage("You Win!", this.width * 5, FontStyle.BOLD, Color.green), 
          this.width * 15, this.height * 15);
      win.placeImageXY(new TextImage("press R to restart", this.width * 3, FontStyle.BOLD, Color.green), 
          this.width * 15, this.height * 25);
      return win;
    }
    for (ArrayList<GamePiece> column : this.board) {
      int y = this.board.indexOf(column) * 30 + (30 / 2);
      for (GamePiece gp : column) {
        int x = column.indexOf(gp) * 30 + (30 / 2);
        w.placeImageXY(gp.tileImage(30, 5, Color.black, gp.powerStation), x, y);      
      }
      if (this.sec < 10) {
        w.placeImageXY(new TextImage(this.minutes + ":" + 0 + this.sec, this.width * 4, 
            FontStyle.BOLD, Color.black), this.width * 5, (this.height + 1) * 29);
      }
      else {
        w.placeImageXY(new TextImage(this.minutes + ":" + this.sec,  this.width * 4, 
            FontStyle.BOLD, Color.black), this.width * 5, (this.height + 1) * 29);  
      }
      w.placeImageXY(new TextImage("Score: " + this.score,  this.width * 4, 
          FontStyle.BOLD, Color.black), this.width * 19, (this.height + 1) * 29); 
      w.placeImageXY(new AboveImage( new TextImage("s-key: scramble board",  this.width * 3, 
          FontStyle.BOLD, Color.black), new TextImage("r-key: new board",  this.width * 3, 
              FontStyle.BOLD, Color.black)), this.width * 18, (this.height + 2) * 29);
    }
    return w;
  }

  //Effect:
  //initialize the minimum spanning tree as random nodes and weights 
  //using the neighbors of each GamePiece on the board
  //mutated fields: mst.
  public void initMst() {
    this.addConnectedNeighbors();
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        GamePiece gp = this.board.get(i).get(j);
        for (GamePiece neighbor : gp.neighbors) {
          if ((i == 0) && (j == 0)) {
            this.mst.add(new Edge(gp, neighbor, 0));
          }
          this.mst.add(new Edge(gp, neighbor, (new Random()).nextInt()));
        }
      }
    }
  }

  //FOR TESTING!!
  //Effect:
  //initialize the minimum spanning tree as random nodes and weights 
  //using the neighbors of each GamePiece on the board
  //mutated fields: mst.
  public void testInitMst() {
    this.addConnectedNeighbors();
    for (int i = 0; i < this.width; i++) {
      for (int j = 0; j < this.height; j++) {
        GamePiece gp = this.board.get(i).get(j);
        for (GamePiece neighbor : gp.neighbors) {
          if ((i == 0) && (j == 0)) {
            this.mst.add(new Edge(gp, neighbor, 0));
          }
          this.mst.add(new Edge(gp, neighbor, (this.ran).nextInt()));
        }
      }
    }
  }

  //Board Generation using Kruskal's algorithm
  public ArrayList<Edge> generateTree() {
    HashMap<GamePiece, GamePiece> representatives = new HashMap<GamePiece, GamePiece>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    ArrayList<Edge> worklist = this.mst;
    Utils util = new Utils();
    worklist.sort(new compareEdges());

    //initialize every node's representative to itself
    for (ArrayList<GamePiece> column : this.board) {
      for (GamePiece gp : column) {
        representatives.put(gp, gp);
      }
    }

    while (worklist.size() > 0) {
      if (util.find(representatives, worklist.get(0).start).equals(
          util.find(representatives, worklist.get(0).end))) {
        worklist.remove(0); 
      }
      else {
        edgesInTree.add(worklist.get(0));
        representatives.replace(util.find(representatives, worklist.get(0).start), 
            util.find(representatives, worklist.get(0).end));
      }
    }
    return edgesInTree;
  }

  //Effect: resets the wires and then mutates the orientation of the wires
  //according to the outEdges of each gamePiece
  void calcWires() {
    for (ArrayList<GamePiece> column : this.board) {
      for (GamePiece gp : column) {
        //calculates the number of wires needed for a piece
        for (Edge e : this.mst) {
          if (e.start.equals(gp) || e.end.equals(gp)) {
            gp.outEdges.add(e);
          }
          gp.bottom = false;
          gp.top = false;
          gp.left = false;
          gp.right = false;
          //set up the wires
          for (Edge oe : gp.outEdges) {
            //if its the start
            if (oe.start.equals(gp)) {
              if (oe.end.col < gp.col) {
                gp.right = true;
              }
              if (oe.end.col > gp.col) {
                gp.left = true;
              }
              if (oe.end.row < gp.row) { 
                gp.top = true;
              }
              if (oe.end.row > gp.row) {
                gp.bottom = true;
              }
            }
            //if its the end
            if (oe.end.equals(gp)) {
              if (oe.start.col < gp.col) {
                gp.right = true;
              }
              if (oe.start.col > gp.col) {
                gp.left = true;
              }

              if (oe.start.row < gp.row) { 
                gp.top = true;
              }
              if (oe.start.row > gp.row) {
                gp.bottom = true;
              }
            }
          }
        }
      }
    }
  }

  //Effect: keeps track of the time passed; mutates the fields minutes and sec
  //to display the time in the form minutes:seconds.
  @Override
  public void onTick() {
    this.updatePower();
    this.sec = this.sec + 1;   
    if (this.sec == 60) {
      this.minutes += 1;
      this.sec = 0;
    }
  }

  //Effect: mutates the neighbors on each GamePiece on the board according to its index
  public void addConnectedNeighbors() {
    for (int i = 0; i < this.width; i = i + 1) {
      for (int j = 0; j < this.height; j = j + 1) {
        //remove the neighbors from before
        this.board.get(i).get(j).neighbors = new ArrayList<GamePiece>();
        //top 
        if ((i - 1) >= 0) {
          if (this.board.get(i - 1).get(j).bottom && this.board.get(i).get(j).top) {
            this.board.get(i).get(j).neighbors.add(
                this.board.get(i - 1).get(j));
          }
        }
        //bottom
        if ((i + 1) < this.width) {
          if (this.board.get(i + 1).get(j).top && this.board.get(i).get(j).bottom) {
            this.board.get(i).get(j).neighbors.add(
                this.board.get(i + 1).get(j));
          }
        }
        //left
        if ((j - 1) >= 0) {
          if (this.board.get(i).get(j - 1).right && this.board.get(i).get(j).left) {
            this.board.get(i).get(j).neighbors.add(
                this.board.get(i).get(j - 1));
          }
        }
        //right
        if (j + 1 < this.height) {
          if (this.board.get(i).get(j + 1).left && this.board.get(i).get(j).right) {
            this.board.get(i).get(j).neighbors.add(
                this.board.get(i).get(j + 1));
          }
        }
      }
    }
  }


  //Effect: keeps track of mouse clicks on the tiles. If a tile is clicked,
  //then it is rotated.
  //Mutation(fields): top, left, down, up
  @Override
  public void onMouseClicked(Posn pos, String buttonName) {
    if (buttonName.equals("LeftButton")) {
      GamePiece tile = this.board.get(Math.floorDiv(pos.y, 30)).get(Math.floorDiv(pos.x, 30));
      tile.rotate();
      //need to update the power
      this.addConnectedNeighbors();
      this.updatePower();
    }
  }

  //returns the distance from the given gamePiece to the  
  //the powerStation
  public Double calcDist(GamePiece gp) {
    double dist = 0;
    int x = gp.row;
    int y = gp.col;
    int x2 = this.powerRow;
    int y2 = this.powerCol;  
    dist = Math.sqrt((Math.pow((x2 - x), 2)) + (Math.pow((y2 - y), 2)));
    return dist;  
  }


  //Effect: updates the power of the tiles on the board
  //if they are connected to the powerStation by some path and within the range of the radius
  //of the powerStation.
  //Mutation: powered
  public void updatePower() {
    for (ArrayList<GamePiece> column : this.board) {
      for (GamePiece gp : column) {
        gp.relDist = this.calcDist(gp) / this.radius;
        if (this.bfs(this.board.get(powerCol).get(powerRow), gp) && (this.calcDist(gp) <= this.radius)) {
          gp.powered = true;
        }
        else {
          gp.powered = false;
        }
      }
    }
  }

  //breath first search algorithm. Determines if there's an available
  //path from one given GamePiece to another one.
  boolean bfs(GamePiece from, GamePiece to) {
    ArrayList<GamePiece> alreadySeen = new ArrayList<GamePiece>();
    ArrayList<GamePiece> worklist = new ArrayList<GamePiece>();

    // Initialize the worklist with the from vertex
    worklist.add(from);
    // As long as the worklist isn't empty...
    while (!worklist.isEmpty()) {
      GamePiece next = worklist.remove(0);
      if (next.equals(to)) {
        return true; // Success!
      }
      else if (alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        // add all the neighbors of next to the worklist for further processing
        for (GamePiece g : next.neighbors) {
          worklist.add(g);
        }
        // add next to alreadySeen, since we're done with it
        alreadySeen.add(0, next);
      }
    }
    // We haven't found the to vertex, and there are no more to try
    return false;
  }


  //determines if all the tiles in the board are powered or not
  public boolean allPowered(ArrayList<ArrayList<GamePiece>> board) {
    int totalPowered = 0;
    int result = this.width * this.height;
    for (ArrayList<GamePiece> column : board) {
      for (GamePiece tile: column) {
        if (tile.powered) {
          totalPowered += 1;
        }
      }
    }
    return totalPowered == result;    
  }



  //Effect: scrambles the board by rotating each tile a random number
  //of times. mutates the fields, top, left, bottom and right.
  public void scrambleBoard() {
    Random rotate = new Random();
    int r = 0;
    for (ArrayList<GamePiece> column : this.board) {
      for (GamePiece tile: column) {
        r = rotate.nextInt(5);
        for (int x = 0; x < r; x++) {
          tile.rotate();
        } 
      }
    }    
  }

  //FOR TESTING!!!!!!!!
  //Effect: scrambles the board by rotating each tile a random number
  //of times. mutates the fields, top, left, bottom and right.
  public void testScrambleBoard(Random ran) {
    int r = 0;
    for (ArrayList<GamePiece> column : this.board) {
      for (GamePiece tile: column) {
        r = ran.nextInt(5);
        for (int x = 0; x < r; x++) {
          tile.rotate();
        } 
      }
    }    
  }

  //Effect: moves the powerStation once an arrow is pressed if it 
  //is connected and inbounds
  //mutates: powerStation, powerCol, powerRow
  public void onKeyEvent(String key) {
    GamePiece current = this.board.get(powerCol).get(powerRow);
    //up key
    if (key.equals("up")) {
      this.updatePower();
      if (this.powerCol > 0) {
        GamePiece above = this.board.get(powerCol - 1).get(powerRow);
        if (current.top && above.bottom) {
          this.score += 1;
          current.powerStation = false;
          this.powerCol -= 1;
          above.powerStation = true;
        }
      }
    }
    //down key
    if (key.equals("down")) {
      this.updatePower();
      if (this.powerCol < this.height - 1) {
        GamePiece below = this.board.get(powerCol + 1).get(powerRow);
        if (current.bottom && below.top) {
          this.score += 1;
          current.powerStation = false;
          this.powerCol += 1;
          below.powerStation = true;
        }
      }
    }
    //left key
    if (key.equals("left")) {
      this.updatePower();
      if (this.powerRow > 0) {
        GamePiece left = this.board.get(powerCol).get(powerRow - 1);
        if (current.left && left.right) {
          this.score += 1;
          current.powerStation = false;
          this.powerRow -= 1;
          left.powerStation = true;
        }
      }
    }
    //right key
    if (key.equals("right")) {
      this.updatePower();
      if (this.powerRow < this.width - 1) {
        GamePiece right = this.board.get(powerCol).get(powerRow + 1);
        if (current.right && right.left) {
          this.score += 1;
          current.powerStation = false;
          this.powerRow += 1;
          right.powerStation = true;
        }
      }
    }
    //scramble the board
    if (key.equals("s")) {
      this.scrambleBoard();
    }

    if (key.equals("r")) {
      this.radius = (int) Math.ceil(this.height / 2) + 1;
      this.sec = 0;
      this.minutes = 0;
      this.mst = new ArrayList<Edge>();
      this.powerCol = 0;
      this.powerRow = 0;
      this.board = new Utils().createBoard(width, height, this.powerRow, this.powerCol); 
      this.nodes = new Utils().createNodes(this.board); 
      this.initMst();
      this.mst = generateTree();
      this.calcWires();
      this.score = 0;
      this.addConnectedNeighbors();
      this.updatePower();
    }
  }
}


//examples
class ExamplesGame {
  ExamplesGame() {
  }

  //test game
  void testScene(Tester t) {
    LightEmAll game = new LightEmAll(4, 4, new Random(3));
    game.bigBang(1000, 1000, 1);
  }

  //test onMouseClicked, neighbors, ScrambleBoard, allpowered
  void testMethods(Tester t) {
    //creates the game
    LightEmAll game = new LightEmAll(3, 3, new Random(3));
    //resets the board
    game.board = new ArrayList<ArrayList<GamePiece>>();
    //rows
    ArrayList<GamePiece> row1 = new ArrayList<GamePiece>();
    ArrayList<GamePiece> row2 = new ArrayList<GamePiece>();
    ArrayList<GamePiece> row3 = new ArrayList<GamePiece>();
    //add the pieces to the board so its allpowered and the powerstation is in the top left
    GamePiece gp1 = new GamePiece(0, 0, true, true, true, true, true);
    GamePiece gp2 = new GamePiece(0, 0, true, true, false, false, false);
    GamePiece gp3 = new GamePiece(0, 0, true, true, false, false, false);
    GamePiece gp4 = new GamePiece(0, 0, true, true, true, true, false);
    GamePiece gp5 = new GamePiece(0, 0, true, true, false, false, false);
    GamePiece gp6 = new GamePiece(0, 0, true, true, false, false, false);
    GamePiece gp7 = new GamePiece(0, 0, true, true, true, true, false);
    GamePiece gp8 = new GamePiece(0, 0, true, true, false, false, false);
    GamePiece gp9 = new GamePiece(0, 0, true, true, false, false, false);
    row1.add(gp1);
    row1.add(gp2);
    row1.add(gp3);
    row2.add(gp4);
    row2.add(gp5);
    row2.add(gp6);
    row3.add(gp7);
    row3.add(gp8);
    row3.add(gp9);
    game.board.add(row1);
    game.board.add(row2);
    game.board.add(row3);
    //adds the neighbors
    game.addConnectedNeighbors();
    //updates the power
    game.updatePower();

    //checks that the corner piece has the two correct neighbors
    t.checkExpect(game.board.get(0).get(0).neighbors, 
        new ArrayList<GamePiece>(Arrays.asList(game.board.get(1).get(0),
            game.board.get(0).get(1))));

    //checks the neighbors of the middle piece to see if it adds the two its connected to
    //and doesnt add the ones above and below its not connected to
    t.checkExpect(game.board.get(1).get(1).neighbors, 
        new ArrayList<GamePiece>(Arrays.asList(game.board.get(1).get(0),
            game.board.get(1).get(2))));

    //checks the given gamepiece if it has a right wire and is powered
    t.checkExpect(game.board.get(0).get(1).right, true);
    t.checkExpect(game.board.get(0).get(1).powered, true);

    //the whole game board should be powered
    t.checkExpect(game.allPowered(game.board), true);

    //clicks on the top middle tile
    game.onMouseClicked(new Posn(45, 0), "LeftButton");

    //should rotate so bottom should be tru and it shouldnt have a right anymore 
    t.checkExpect(game.board.get(0).get(1).bottom, true);
    t.checkExpect(game.board.get(0).get(1).right, false);

    //the power should not work anymore if its not all connected
    t.checkExpect(game.board.get(0).get(1).powered, false);
    //the nieghbors should update during onClick
    t.checkExpect(game.board.get(0).get(1).neighbors, new ArrayList<GamePiece>());
    //the game should no longer be all powered
    t.checkExpect(game.allPowered(game.board), false);

    //randomize the board
    t.checkExpect(gp5.top, false);
    t.checkExpect(gp5.bottom, false);
    t.checkExpect(gp5.left, true);
    t.checkExpect(gp5.right, true);
    t.checkExpect(gp8.top, false);
    t.checkExpect(gp8.bottom, false);
    t.checkExpect(gp8.left, true);
    t.checkExpect(gp8.right, true);

    //scramble
    game.testScrambleBoard(game.ran);

    //doesnt change/ but its random
    t.checkExpect(gp5.top, true);
    t.checkExpect(gp5.bottom, true);
    t.checkExpect(gp5.left, false);
    t.checkExpect(gp5.right, false);
    //changes direction but keeps shape
    t.checkExpect(gp8.top, false);
    t.checkExpect(gp8.bottom, false);
    t.checkExpect(gp8.left, true);
    t.checkExpect(gp8.right, true);
  }


  //tests for onkey
  void testOnKey(Tester t) {
    //game
    LightEmAll game = new LightEmAll(10, 10, new Random(3));
    game.testScrambleBoard(game.ran);
    //checks to see that it sstarts in the middle
    t.checkExpect(game.powerCol, 0);
    t.checkExpect(game.powerRow, 0);
    //left key
    game.onKeyEvent("left");
    //checks to see that it moves to the left
    t.checkExpect(game.powerCol, 0);
    t.checkExpect(game.powerRow, 0);
    //move to the right
    game.onKeyEvent("right");
    game.onKeyEvent("right");
    //moves to the right twice
    t.checkExpect(game.powerRow, 1);
    //set to the edge of the board
    game.powerRow = 9;
    //check to see that it doesnt go out of bounds
    game.onKeyEvent("right");
    //doesnt change
    t.checkExpect(game.powerRow, 9);
    //set to bottom of board
    game.powerCol = 9;
    //up
    game.onKeyEvent("up");
    //doesnt move up one because its not connected
    t.checkExpect(game.powerCol, 8);
    //to prevent from going out of bounds
    game.onKeyEvent("down");
    t.checkExpect(game.powerCol, 9);
  }

  //tests for bfs
  void testBfs(Tester t) {
    LightEmAll game = new LightEmAll(10, 10, new Random(3));
    game.addConnectedNeighbors();
    t.checkExpect(game.bfs(game.board.get(5).get(5), 
        game.board.get(5).get(3)), false);
    t.checkExpect(game.bfs(game.board.get(5).get(5), 
        game.board.get(6).get(3)), false);
    t.checkExpect(game.bfs(game.board.get(0).get(0), 
        game.board.get(6).get(2)), false);
  }

  //tests for tileimage
  void testTileImage(Tester t) {
    //Test case for an unpowered GamePiece with no connections
    GamePiece tile1 = new GamePiece(0, 0, false, false, false, false, false);
    WorldImage expected = new OverlayImage(
        new RectangleImage(10, 10, OutlineMode.SOLID, Color.DARK_GRAY),
        new RectangleImage(100, 100, OutlineMode.SOLID, Color.DARK_GRAY)
        );

    t.checkExpect(tile1.tileImage(100, 10, Color.DARK_GRAY, false), expected);

    //Test case for a powered GamePiece with all connections
    GamePiece tile2 = new GamePiece(0, 0, true, true, true, true, true);
    int size = 100;
    int wireWidth = 10;
    Color wireColor = Color.YELLOW;
    WorldImage expectedWireColor = new RectangleImage(
        wireWidth, wireWidth, OutlineMode.SOLID, wireColor);
    WorldImage expectedTileColor = new RectangleImage(
        size, size, OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage vWire = new RectangleImage(50, 10, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage(10, 50, OutlineMode.SOLID, wireColor);
    WorldImage expected2 = new OverlayOffsetAlign(
        AlignModeX.LEFT, AlignModeY.MIDDLE, vWire, 0, 0,
        new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, hWire, 0, 0,
            new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, vWire, 0, 0,
                new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, hWire, 0, 0,
                    new OverlayImage(expectedWireColor, expectedTileColor)))));
    t.checkExpect(tile2.tileImage(size, wireWidth, wireColor, false), expected2);

    //Test case for a GamePiece with the power station
    GamePiece powerstation = new GamePiece(0, 0, false, false, false, false, true);
    Color wireColor1 = Color.DARK_GRAY;
    WorldImage expectedWireColor1 = new RectangleImage(
        wireWidth, wireWidth, OutlineMode.SOLID, wireColor1);
    WorldImage expectedTileColor1 = new RectangleImage(
        size, size, OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage powerStationImage = new OverlayImage(
        new OverlayImage(
            new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
            new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))
            ),
        new OverlayImage(expectedWireColor1, expectedTileColor1)
        );
    t.checkExpect(powerstation.tileImage(size, wireWidth, wireColor1, true), powerStationImage);


    //Test case for an unpowered GamePiece with some connections
    GamePiece tile3 = new GamePiece(0, 0, true, false, true, false, false);
    WorldImage expectedWireColor2 = new RectangleImage(
        wireWidth, wireWidth, OutlineMode.SOLID, wireColor1);
    WorldImage expectedTileColor2 = new RectangleImage(
        size, size, OutlineMode.SOLID, Color.DARK_GRAY);
    WorldImage vWire2 = new RectangleImage(50, 10, OutlineMode.SOLID, wireColor1);
    WorldImage hWire2 = new RectangleImage(10, 50, OutlineMode.SOLID, wireColor1);
    WorldImage expected3 = new OverlayOffsetAlign(
        AlignModeX.LEFT, AlignModeY.MIDDLE, vWire2, 0, 0,
        new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, hWire2, 0, 0,
            new OverlayImage(expectedWireColor2, expectedTileColor2)));

    t.checkExpect(tile3.tileImage(size, wireWidth, wireColor1, false), expected3);


    //Test case for a powered GamePiece with some connections
    GamePiece tile4 = new GamePiece(0, 0, true, false, true, false, false);
    WorldImage expected4 = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, vWire, 0, 0,
        new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, hWire, 0, 0,
            new OverlayImage(expectedWireColor, expectedTileColor)));

    t.checkExpect(tile4.tileImage(size, wireWidth, wireColor, false), expected4);
  }

  //test for update power
  void testMakeScene(Tester t) {
    LightEmAll game = new LightEmAll(3, 3);
    game.board.get(0).get(0).powered = true;
    game.board.get(0).get(1).powered = true;
    game.board.get(0).get(2).powered = true;
    game.board.get(1).get(0).powered = true;
    game.board.get(1).get(1).powered = true;
    game.board.get(1).get(2).powered = true;
    game.board.get(2).get(0).powered = true;
    game.board.get(2).get(1).powered = true;
    game.board.get(2).get(2).powered = true;   
    WorldScene win = new WorldScene(90, 90);
    WorldScene w = new WorldScene(90, 90);
    win.placeImageXY(new TextImage("You Win!", game.width * 5, FontStyle.BOLD, Color.green), 
        game.width * 15, game.height * 15);
    win.placeImageXY(new TextImage("press R to restart", game.width * 3, FontStyle.BOLD, Color.green), 
        game.width * 15, game.height * 25);
    t.checkExpect(game.makeScene(), win);

    game.board.get(0).get(0).powered = false;
    game.board.get(0).get(0).powerStation = false;
    game.board.get(0).get(0).right = true;
    game.board.get(0).get(0).left = true;
    game.board.get(0).get(1).powered = false;
    game.board.get(0).get(1).top = true;
    game.board.get(0).get(1).bottom = true;
    game.board.get(0).get(2).powered = false;
    game.board.get(0).get(2).top = true;
    game.board.get(0).get(2).right = true;
    game.board.get(1).get(0).right = true;
    game.board.get(1).get(0).bottom = true;
    game.board.get(1).get(1).powerStation = true;
    game.board.get(1).get(1).top = true;
    game.board.get(1).get(1).right = true;
    game.board.get(1).get(2).powered = false;
    game.board.get(1).get(2).right = true;
    game.board.get(1).get(2).left = true;
    game.board.get(2).get(0).left = true;
    game.board.get(2).get(0).bottom = true;
    game.board.get(2).get(1).top = true;
    game.board.get(2).get(1).left = true;
    game.board.get(2).get(2).powered = false;
    game.board.get(2).get(2).top = true;
    game.board.get(2).get(2).left = true;

    w.placeImageXY(game.board.get(0).get(0).tileImage(30, 5, Color.BLACK, false), 15, 15);
    w.placeImageXY(game.board.get(0).get(1).tileImage(30, 5, Color.BLACK, false), 45, 15);
    w.placeImageXY(game.board.get(0).get(2).tileImage(30, 5, Color.BLACK, false), 75, 15);
    w.placeImageXY(game.board.get(1).get(0).tileImage(30, 5, Color.BLACK, false), 15, 45);
    w.placeImageXY(game.board.get(1).get(1).tileImage(30, 5, Color.BLACK, true), 45, 45);
    w.placeImageXY(game.board.get(1).get(2).tileImage(30, 5, Color.BLACK, false), 75, 45);
    w.placeImageXY(game.board.get(2).get(0).tileImage(30, 5, Color.BLACK, false), 15, 75);
    w.placeImageXY(game.board.get(2).get(1).tileImage(30, 5, Color.BLACK, false), 45, 75);
    w.placeImageXY(game.board.get(2).get(2).tileImage(30, 5, Color.BLACK, false), 75, 75);
    if (game.sec < 10) {
      w.placeImageXY(new TextImage(game.minutes + ":" + 0 + game.sec, game.width * 4, 
          FontStyle.BOLD, Color.black), game.width * 5, (game.height + 1) * 29);
    }
    else {
      w.placeImageXY(new TextImage(game.minutes + ":" + game.sec,  game.width * 4, 
          FontStyle.BOLD, Color.black), game.width * 5, (game.height + 1) * 29);  
    }
    w.placeImageXY(new TextImage("Score: " + game.score,  game.width * 4, 
        FontStyle.BOLD, Color.black), game.width * 19, (game.height + 1) * 29); 
    w.placeImageXY(new AboveImage( new TextImage("s-key: scramble board",  game.width * 3, 
        FontStyle.BOLD, Color.black), new TextImage("r-key: new board",  game.width * 3, 
            FontStyle.BOLD, Color.black)), game.width * 18, (game.height + 2) * 29);
    
    t.checkExpect(game.makeScene(), w);    
  }

  //test for update power
  void testUpdatePower(Tester t) {
    LightEmAll game = new LightEmAll(3, 3);
    game.board.get(0).get(0).right = true;
    game.board.get(0).get(0).left = true;
    game.board.get(0).get(1).top = true;
    game.board.get(0).get(1).bottom = true;
    game.board.get(0).get(2).top = true;
    game.board.get(0).get(2).right = true;
    game.board.get(1).get(0).right = true;
    game.board.get(1).get(0).bottom = true;
    game.board.get(1).get(2).right = true;
    game.board.get(1).get(2).left = true;
    game.board.get(2).get(0).left = true;
    game.board.get(2).get(0).bottom = true;
    game.board.get(2).get(1).top = true;
    game.board.get(2).get(1).left = true;
    game.board.get(2).get(2).top = true;
    game.board.get(2).get(2).left = true;

    game.updatePower();
    t.checkExpect(game.board.get(0).get(0).powered, true);
    t.checkExpect(game.board.get(0).get(1).powered, false);
    t.checkExpect(game.board.get(0).get(2).powered, false);
    t.checkExpect(game.board.get(1).get(0).powered, false);
    t.checkExpect(game.board.get(1).get(1).powered, false);
    t.checkExpect(game.board.get(1).get(2).powered, false);
    t.checkExpect(game.board.get(2).get(0).powered, false);
    t.checkExpect(game.board.get(2).get(1).powered, false);
    t.checkExpect(game.board.get(2).get(2).powered, false);
  }
  
  void testCalcDist(Tester t) {  
    LightEmAll game = new LightEmAll(3, 3, new Random(3));
    GamePiece gp1 = new GamePiece(0, 0, true, true, false, false, false);  
    GamePiece gp2 = new GamePiece(4, 5, true, true, false, false, false); 
    t.checkExpect(game.calcDist(gp1), 0.0);
    t.checkExpect(game.calcDist(gp2), 6.4031242374328485);
    
  }
  
  void testOnTick(Tester t) {
    LightEmAll game = new LightEmAll(4, 4, new Random(4));
    t.checkExpect(game.minutes, 0);
    t.checkExpect(game.sec, 0);
    game.onTick();
    t.checkExpect(game.minutes, 0);
    t.checkExpect(game.sec, 1);
    game.sec = 59;
    game.onTick();
    t.checkExpect(game.minutes, 1);
    t.checkExpect(game.sec, 0);
    game.sec = 34;
    game.minutes = 6;
    t.checkExpect(game.minutes, 6);
    t.checkExpect(game.sec, 34);     
  }  
  
  void testCreateBoard(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, true, true, true, true, true);
    GamePiece gp2 = new GamePiece(0, 1, true, true, true, true, false);
    GamePiece gp4 = new GamePiece(1, 0, true, true, true, true, false);
    GamePiece gp5 = new GamePiece(1, 1, true, true, true, true, false);
    ArrayList<GamePiece> cols1 = new ArrayList<GamePiece>(Arrays.asList(gp1, gp4));
    ArrayList<GamePiece> cols2 = new ArrayList<GamePiece>(Arrays.asList(gp2, gp5));  
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(cols1, cols2));   
    t.checkExpect(new Utils().createBoard(2, 2, 0, 0), board);    
    
  }
  
  void testCreateNodes(Tester t) {
    ArrayList<ArrayList<GamePiece>> empty = new ArrayList<ArrayList<GamePiece>>();  
    GamePiece gp1 = new GamePiece(0, 0, true, true, true, true, true);
    GamePiece gp2 = new GamePiece(0, 1, true, true, true, true, false);
    GamePiece gp4 = new GamePiece(1, 0, true, true, true, true, false);
    GamePiece gp5 = new GamePiece(1, 1, true, true, true, true, false);
    ArrayList<GamePiece> cols1 = new ArrayList<GamePiece>(Arrays.asList(gp1, gp4));
    ArrayList<GamePiece> cols2 = new ArrayList<GamePiece>(Arrays.asList(gp2, gp5));  
    ArrayList<ArrayList<GamePiece>> board = new ArrayList<ArrayList<GamePiece>>(Arrays.asList(cols1, cols2));  
    ArrayList<GamePiece> nodes = new ArrayList<GamePiece>(Arrays.asList(gp1, gp4, gp2, gp5));
    ArrayList<GamePiece> emptyN = new ArrayList<GamePiece>();
    t.checkExpect(new Utils().createNodes(empty), emptyN);    
    t.checkExpect(new Utils().createNodes(board), nodes);     
  }  
  
  void testRotate(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, true, true, true, true, true);
    gp1.rotate();
    t.checkExpect(gp1.top, true); 
    t.checkExpect(gp1.bottom, true); 
    t.checkExpect(gp1.left, true); 
    t.checkExpect(gp1.right, true);   
    GamePiece gp2 = new GamePiece(0, 0, false, false, false, false, false);
    gp2.rotate();
    t.checkExpect(gp2.top, false); 
    t.checkExpect(gp2.bottom, false); 
    t.checkExpect(gp2.left, false); 
    t.checkExpect(gp2.right, false); 
    GamePiece gp3 = new GamePiece(0, 0, true, false, true, false, false);
    gp3.rotate();
    t.checkExpect(gp3.top, false); 
    t.checkExpect(gp3.bottom, true); 
    t.checkExpect(gp3.left, true); 
    t.checkExpect(gp3.right, false); 
    gp3.rotate();
    t.checkExpect(gp3.top, false); 
    t.checkExpect(gp3.bottom, true); 
    t.checkExpect(gp3.left, false); 
    t.checkExpect(gp3.right, true);   
  }
  
  void testCompareEdges(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, true, true, false, true, true);
    GamePiece gp2 = new GamePiece(0, 0, true, true, true, true, false);
    Edge e1 = new Edge(gp1, gp2, 40);
    Edge e2 = new Edge(gp2, gp1, 50);
    t.checkExpect(new compareEdges().compare(e1, e1), 0);
    t.checkExpect(new compareEdges().compare(e1, e2), -10);
    t.checkExpect(new compareEdges().compare(e2, e1), 10);      
  }
}




//inRange(GamePiece gp)
//check if distance between gp and powerstation is less than or equal to radius










