package com.example.supermariobros;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        gameView = new GameView(this);
        setContentView(gameView);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //NEED LOADER FOR PANNING TO WORK
        //ScrollingImageView scrollingBackground = (ScrollingImageView) loader.findViewById(R.id.scrolling_background);
        //scrollingBackground.stop();
        //scrollingBackground.start();
    }

    @Override
    protected void onResume(){
        super.onResume();
        gameView.resume();
    }

    protected void onPause(){
        super.onPause();
        gameView.pause();
    }

    class GameView extends SurfaceView implements Runnable{

        private Thread gameThread;
        private SurfaceHolder ourHolder;
        private volatile boolean playing;
        private Canvas canvas;
        private Bitmap bitmapMario;
        private Bitmap bitmapGoomba;
        private Bitmap bitmapPiranha;
        private Bitmap bitmapStar;
        private Bitmap bitmapCoin;
        private Bitmap bitmapMap;
        private Bitmap button1;
        private Bitmap button2;
        private Bitmap button3;
        private Bitmap jumpbutton;
        private Bitmap leftbutton;
        private Bitmap rightbutton;
        private boolean isMoving;
        private float runSpeedPerSecond = 200;            // change running speed
        private float manXPos = 10, manYPos = 880;        // starting position
        private float tmpmanYPos = 880;
        private float goombaXPos = 200, goombaYPos = 975; //970 safety for goomba
        private float piranhaXPos = 1165, piranhaYPos = 750;

        private float starXPos = 1000, starYPos = 700;
        private float coinXPos = 1000, coinYPos = 975;
        private int frameWidth = 100, frameHeight = 170; // change character size
        private int goombaWidth = 95, goombaHeight = 110;
        private int starWidth = 50;
        private int coinWidth = 50;
        private int frameCount = 3; //5 for mariorun                    // number of frames of character (5 images of char in row)
        private int goombaFrames = 6;
        private int piranhaFrames = 3;
        private int coinFrames = 4;
        //private float starXPos = 1250, starYPos = 900;

        private int starFrames = 1;
        private int currentFrame = 0;
        private long fps;
        private long timeThisFrame;
        private long lastFrameChangeTime = 0;
        private int frameLengthInMillisecond = 170;
        private int starFrameInMillisecond = 20;
        private int x_dir = 5;
        private int map_x = 0;
        private int mapflag = 0;
        private int displayDeath = 0;
        private int jumpflag = 3;
        private int leftflag = 0;
        private int rightflag = 0;
        private int stopflag = 0;

        public int score = 0;
        public int lives = 3;
        public int level = 0;
        public int marioform = 1;

        int mapdata[][] = new int[8000][1080]; // [x][y]
        // empty = 0, smallmario = 1, supermario = 2, starmario = 3, powerup = 4, obstacle = 5, enemy = 6, star = 7
        // 1 x 4 -> 2, 1 x 7 -> 3, 2 x 7 -> 3, 1 x 6 -> dead (unless jump), 2 x 6 -> 1 (unless jump), 3 x 6 -> enemy dead, 1/2/3 x 5 -> new location


        private Rect frameToDraw = new Rect(0,0,frameWidth, frameHeight);
        private RectF whereToDraw = new RectF(manXPos, manYPos, manXPos + frameWidth, frameHeight);

        private Rect screensize = new Rect(0, 0, 1920, 1080);

        public GameView(Context context){
            super(context);
            ourHolder = getHolder();

            bitmapMario = BitmapFactory.decodeResource(getResources(), R.drawable.marioright); //mariorun old version
            bitmapMario = Bitmap.createScaledBitmap(bitmapMario, frameWidth * frameCount, frameHeight, false);

            bitmapGoomba = BitmapFactory.decodeResource(getResources(), R.drawable.goomba3);
            bitmapGoomba = Bitmap.createScaledBitmap(bitmapGoomba, goombaWidth * goombaFrames, goombaHeight, false);

            bitmapPiranha = BitmapFactory.decodeResource(getResources(), R.drawable.piranha);
            bitmapPiranha = Bitmap.createScaledBitmap(bitmapPiranha, frameWidth * piranhaFrames, frameHeight, false);

            //bitmapCoin = BitmapFactory.decodeResource(getResources(), R.drawable.coin);
            //bitmapCoin = Bitmap.createScaledBitmap(bitmapCoin, 100 * coinFrames, 80, false);

            bitmapStar = BitmapFactory.decodeResource(getResources(), R.drawable.star);
            bitmapStar = Bitmap.createScaledBitmap(bitmapStar, frameWidth * starFrames, frameHeight, false);

        }

        @Override
        public void run(){
            while(playing){
                long startFrameTime = System.currentTimeMillis();
                update();
                draw();

                timeThisFrame = System.currentTimeMillis() - startFrameTime;

                if(timeThisFrame >= 1){
                    fps = 1000/timeThisFrame;
                }
            }
        }

        public void update(){
            if(isMoving){
                if((leftflag == 1) && (manXPos > 0)){ //move left and bound mario to edge of screen
                    manXPos = manXPos - runSpeedPerSecond / fps;
                }
                if(rightflag == 1){
                    manXPos = manXPos + runSpeedPerSecond / fps;
                    piranhaXPos = piranhaXPos - 15;
                    starXPos = starXPos - 15;
                }
                goombaXPos = goombaXPos + runSpeedPerSecond / fps;

                /*if (piranhaYPos > 300 && jumpflag == 0){
                    piranhaYPos = piranhaYPos - runSpeedPerSecond + 10 / fps;
                    if (piranhaYPos <= 300){
                        jumpflag = 1;
                        System.out.println("HIT TOP");
                    }
                }

                else if (jumpflag == 1){
                    piranhaYPos = piranhaYPos + runSpeedPerSecond + 10 / fps;
                    if (piranhaYPos >= 750){
                        jumpflag = 2;
                        System.out.println("HIT BOTTOM");
                    }
                }*/

                if (manYPos > 650 && jumpflag == 0){
                    manYPos = manYPos - runSpeedPerSecond + 10 / fps;
                    if (manYPos <= 650){
                        jumpflag = 1;
                        System.out.println("HIT TOP");
                    }
                }

                else if (jumpflag == 1){
                    manYPos = manYPos + runSpeedPerSecond + 10 / fps;
                    if (manYPos >= 880){
                        jumpflag = 2;
                        System.out.println("HIT BOTTOM");
                        manYPos = 880; // do not allow mario to fall below ground, need to check gaps
                    }
                }

                if(manXPos > getWidth()){
                    manYPos = manYPos; // keep mario in the bottom of screen
                    manXPos = 10;
                    score = score + 100;
                    //mapflag = 1;
                }
                if(goombaXPos > getWidth()){
                    goombaXPos = 10;
                }

                //USE THIS TO KILL BY FALL (FALL BELOW GROUND)
                if(manYPos + frameHeight > getHeight()){
                    manYPos = manYPos;
                    lives = lives - 1;
                    displayDeath = 1;
                }

                //USE THIS TO KILL BY FALL (FALL BELOW GROUND)
                if((goombaYPos - 5) + goombaHeight > getHeight()){
                    goombaYPos = goombaYPos;
                }
            }
        }

        public void manageCurrentFrame(){
            long time = System.currentTimeMillis();

            if(isMoving){
                if(time > lastFrameChangeTime + frameLengthInMillisecond){
                    lastFrameChangeTime = time;
                    currentFrame++;

                    if(currentFrame >= frameCount){
                        currentFrame = 0;
                    }
                    if(currentFrame >= goombaFrames){
                        currentFrame = 0;
                    }
                }
            }

            frameToDraw.left = currentFrame * frameWidth;
            frameToDraw.right = frameToDraw.left + frameWidth;
        }

        public void draw(){
            if(ourHolder.getSurface().isValid()){
                canvas = ourHolder.lockCanvas();
                Bitmap bitmapMap = BitmapFactory.decodeResource(getResources(), R.drawable.mariomapfull);
                button1 = BitmapFactory.decodeResource(getResources(), R.drawable.buttonleft);
                button2 = BitmapFactory.decodeResource(getResources(), R.drawable.buttonright);
                button3 = BitmapFactory.decodeResource(getResources(), R.drawable.button);
                jumpbutton = Bitmap.createScaledBitmap(button3, 150, 150, false);
                leftbutton = Bitmap.createScaledBitmap(button1, 150, 150, false);
                rightbutton = Bitmap.createScaledBitmap(button2, 150, 150, false);

                bitmapMap = Bitmap.createScaledBitmap(bitmapMap, bitmapMap.getWidth(), 1175, false);
                canvas.drawColor(Color.CYAN);

                whereToDraw.set((int) manXPos, (int) manYPos, (int) manXPos + frameWidth, (int) manYPos + frameHeight);
                manageCurrentFrame();

                // move until end of map is reached
                if ((mapflag == 1) && (map_x > -7150) ) {
                    /* Trying to get pipe collision
                    if (map_x <= -330 && map_x >= -510){
                        //manXPos >= 743 && manXPos <= 760 && manYPos >= 800
                        isMoving = false;
                        stopflag = 1;
                    }
                    else{
                        stopflag = 0;
                    }
                    //map_x = map_x - x_dir;
                    if (stopflag == 0){ */
                    map_x = map_x - 15;/*}*/

                    //System.out.println("x:" + map_x);
                }

                //TESTING
                //if(map_x == -600){
                //    manYPos = manYPos + 100;
                //}


                else if((map_x < -7150) || (leftflag == 1)){
                    mapflag = 0;
                    if((map_x < -7150)&&(manXPos > 1100)){
                        isMoving = false;
                    }
                }
                //System.out.println("manxPos:" + manXPos);
                //System.out.println("manyPos:" + manYPos);
                System.out.println("map_x:" + map_x);

                canvas.drawBitmap(bitmapMap, map_x, 0, null);

                //draw mario bitmap (directional)
                //check condition for marioform here --> form 1 (babyleft/right) , form 2 (marioleft/right), form 3 (starleft/right)
                //if babyleft/right --> adjust manYPos, height of character
                if(rightflag == 1){
                    bitmapMario = BitmapFactory.decodeResource(getResources(), R.drawable.marioright);
                    bitmapMario = Bitmap.createScaledBitmap(bitmapMario, frameWidth * frameCount, frameHeight, false);
                }
                if(leftflag == 1){
                    bitmapMario = BitmapFactory.decodeResource(getResources(), R.drawable.marioleft);
                    bitmapMario = Bitmap.createScaledBitmap(bitmapMario, frameWidth * frameCount, frameHeight, false);
                }
                canvas.drawBitmap(bitmapMario, frameToDraw, whereToDraw, null);

                //draw goomba bitmap
                whereToDraw.set((int) goombaXPos, (int) goombaYPos, (int) goombaXPos + goombaWidth, (int) goombaYPos + goombaHeight);
                manageCurrentFrame();
                canvas.drawBitmap(bitmapGoomba, frameToDraw, whereToDraw, null);

                //draw buttons
                canvas.drawBitmap(leftbutton, 75, 800, null);
                canvas.drawBitmap(rightbutton, 275, 800, null);
                canvas.drawBitmap(jumpbutton, 175, 650, null);

                //draw piranhas
                whereToDraw.set((int) piranhaXPos, (int) piranhaYPos, (int) piranhaXPos + frameWidth, (int) piranhaYPos + frameHeight);
                canvas.drawBitmap(bitmapPiranha, frameToDraw, whereToDraw, null);


                //draw coin
                whereToDraw.set((int) coinXPos, (int) coinYPos, (int) coinXPos + coinWidth, (int) coinYPos + frameHeight);
                canvas.drawBitmap(bitmapCoin, frameToDraw, whereToDraw, null);

                //draw star
                //if((map_x < -1500)) {
                    //System.out.println("Drawing star...");
                    whereToDraw.set((int) starXPos, (int) starYPos, (int) starXPos + frameWidth, (int) starYPos + frameHeight);
                    int hold1 = frameLengthInMillisecond;
                    frameLengthInMillisecond = starFrameInMillisecond;
                    manageCurrentFrame();
                    canvas.drawBitmap(bitmapStar, frameToDraw, whereToDraw, null);
                    frameLengthInMillisecond = hold1;
               // }


                // draw score
                Paint paint= new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(65);  //set text size
                String s = "0000";
                s = String.valueOf(Integer.parseInt(s) + score);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(s, 140, 165 ,paint);

                // draw world level
                Paint paint6= new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(65);  //set text size
                String s6 = "WORLD 1-1";
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(s6, 1600, 70 ,paint);

                // draw lives
                if(lives > 0) {
                    Paint paint2 = new Paint();
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(65);  //set text size
                    String s2 = "MARIO  x";
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(s2 + lives, 160, 70, paint);
                }

                // draw game over
                if(lives == 0) {
                    Paint paint5 = new Paint();
                    paint.setColor(Color.YELLOW);
                    paint.setTextSize(100);  //set text size
                    String s5 = "GAME OVER!";
                    paint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(s5, 950, 150, paint);
                    isMoving = false; //stops mario and map from moving anymore
                    mapflag = 0;
                }
                // draw mario died
                if(displayDeath == 1) {
                    Paint paint4 = new Paint();
                    paint4.setColor(Color.RED);
                    paint4.setTextSize(100);  //set text size
                    String s4 = "YOU DIED!";
                    paint4.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(s4, 950, 150, paint4);
                    displayDeath = 0;
                    manYPos = tmpmanYPos; //reset mario to ground level
                }

                // draw level clear (when end of map AND mario passes flag)
                if((map_x < -7100) && (manXPos > 1100)){
                    Paint paint3 = new Paint();
                    paint3.setColor(Color.YELLOW);
                    paint3.setTextSize(100);  //set text size
                    String s3 = "STAGE CLEAR!";
                    paint3.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(s3, 950, 150, paint3);
                }

                // close canvas
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void pause(){
            playing = false;

            try{
                gameThread.join();
            } catch(InterruptedException e){
                Log.e("ERR", "Joining Thread");
            }
        }

        public void resume(){
            playing = true;

            gameThread = new Thread(this);
            gameThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event){
            int action = event.getAction();
            float x = event.getX();  // or getRawX();
            float y = event.getY();

            switch(action){
                case MotionEvent.ACTION_DOWN:
                    if (x >= 75 && x < (75 + leftbutton.getWidth())
                            && y >= 800 && y < (800 + leftbutton.getHeight())) {
                        isMoving = !isMoving;
                        leftflag = 1;
                        rightflag = 0; //stop moving right
                        mapflag = 0;   //stop map movement
                        //Left button is clicked
                    }
                    if (x >= 275 && x < (275 + rightbutton.getWidth())
                            && y >= 800 && y < (800 + rightbutton.getHeight())) {
                        isMoving = !isMoving;
                        rightflag = 1;
                        leftflag = 0; //stop moving left
                        mapflag = 1;  //start map movement
                        if((isMoving == false)&& (rightflag == 1)){  //cancel map movement if standstill
                            mapflag = (mapflag + 1) % 2;
                        }
                        //Right button is clicked
                    }
                    if (x >= 175 && x < (175 + jumpbutton.getWidth())
                            && y >= 650 && y < (650 + rightbutton.getHeight())) {
                        jumpflag = 0;
                        //Jump button is clicked
                    }
                    break;
            }
            return true;
        }
    }
}
