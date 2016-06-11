package com.italankin.fifteen;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.DecelerateInterpolator;

public class Tile {

    /**
     * Paint для рисования текста
     */
    private static Paint sPaintText;
    /**
     * Paint для рисования фона плитки
     */
    private static Paint sPaintPath;
    /**
     * Rect для определения границ текста
     */
    private static Rect mRectBounds = new Rect();

    /**
     * Path для рисования фона плитки
     */
    private Path mShape;
    /**
     * RectF для определения границ плитки и создания пути
     */
    private RectF mRectShape;

    /**
     * Индекс плитки в общем массиве
     */
    private int mIndex;
    /**
     * Позиция X плитки на поле
     */
    private float mCanvasX = 0.0f;
    /**
     * Позиция Y плитки на поле
     */
    private float mCanvasY = 0.0f;

    private ObjectAnimator mTranslateXAnimator;
    private ObjectAnimator mTranslateYAnimator;
    private ValueAnimator mScaleAnimator;
    private float mTextScale = 1.0f;
    private Path mDrawPath = new Path();

    private Matrix mMatrix = new Matrix();
    private String mDataText;

    public static void updatePaint() {
        if (sPaintText == null) {
            sPaintText = new Paint();
            sPaintText.setTypeface(Settings.typeface);
            sPaintText.setTextAlign(Paint.Align.CENTER);
            sPaintText.setTextSize(Dimensions.tileFontSize);
        }

        if (sPaintPath == null) {
            sPaintPath = new Paint();
        }

        sPaintPath.setColor(Colors.getTileColor());
        sPaintPath.setAntiAlias(Settings.antiAlias);

        sPaintText.setColor(Colors.getTileTextColor());
        sPaintText.setAntiAlias(Settings.antiAlias);
    }

    /**
     * @param number данные для отображения
     * @param index  индекс в общем массиве
     */
    public Tile(int number, int index) {
        mIndex = index;
        mDataText = Integer.toString(number);

        updatePaint();

        mRectShape = new RectF();
        mShape = new Path();

        mCanvasX = Dimensions.fieldMarginLeft +
                (Dimensions.tileSize + Dimensions.spacing) * (mIndex % Settings.gameWidth);
        mCanvasY = Dimensions.fieldMarginTop +
                (Dimensions.tileSize + Dimensions.spacing) * (mIndex / Settings.gameWidth);

        mShape.addRoundRect(
                new RectF(mCanvasX, mCanvasY,
                        mCanvasX + Dimensions.tileSize, mCanvasY + Dimensions.tileSize),
                Dimensions.tileCornerRadius, Dimensions.tileCornerRadius,
                Path.Direction.CW);

        mShape.computeBounds(mRectShape, false);
        mDrawPath.set(mShape);

        TimeInterpolator interpolator = new DecelerateInterpolator(1.5f);
        mTranslateXAnimator = ObjectAnimator.ofFloat(this, "canvasX", 0, 0);
        mTranslateXAnimator.setInterpolator(interpolator);
        mTranslateYAnimator = ObjectAnimator.ofFloat(this, "canvasY", 0, 0);
        mTranslateYAnimator.setInterpolator(interpolator);

        mScaleAnimator = ObjectAnimator.ofFloat(this, "scale", 1, 1);
        mScaleAnimator.setInterpolator(interpolator);
    } // constructor

    public void draw(Canvas canvas) {
        canvas.drawPath(mDrawPath, sPaintPath);

        if (!Game.isPaused()) {
            sPaintText.setTextSize(mTextScale * Dimensions.tileFontSize);
            sPaintText.getTextBounds(mDataText, 0, mDataText.length(), mRectBounds);
            if (!Settings.hardmode || Game.getMoves() == 0 || Game.isSolved()) {
                canvas.drawText(mDataText, mRectShape.centerX(),
                        mRectShape.centerY() - mRectBounds.centerY(), sPaintText);
            }
        }
    }

    /**
     * @return индекс данного спрайта в общем массиве
     */
    public int getIndex() {
        return mIndex;
    }

    /**
     * Функция для опредления принадлежности координат спрайту
     *
     * @param x координата x
     * @param y координата y
     * @return принадлежат ли координаты {@link #mRectShape}
     */
    public boolean at(float x, float y) {
        return mRectShape.contains(x, y);
    }

    public void setCanvasX(float newX) {
        float dx = newX - mCanvasX;
        mShape.offset(dx, 0);
        mCanvasX = newX;
        updatePath();
    }

    public void setCanvasY(float newY) {
        float dy = newY - mCanvasY;
        mShape.offset(0, dy);
        mCanvasY = newY;
        updatePath();
    }

    public void setScale(float scale) {
        mTextScale = scale;
        updatePath();
        mMatrix.reset();
        mMatrix.setScale(scale, scale, mRectShape.centerX(), mRectShape.centerY());
        mDrawPath.transform(mMatrix);
    }

    private void updatePath() {
        mShape.computeBounds(mRectShape, false);
        mDrawPath.set(mShape);
    }

    /**
     * Вызывается при нажатии на спрайт на экране
     *
     * @return <b>true</b>, если
     */
    public boolean onClick() {
        // получение текущих координат спрайта на поле
        int x = mIndex % Settings.gameWidth;
        int y = mIndex / Settings.gameWidth;

        // новый индекс спрайта после перемещения
        int newIndex = Game.move(x, y);

        // если текущий индекс не равен новому индексу
        // (т.е. был сделан ход и ситуация на поле изменилась)
        if (mIndex != newIndex) {
            mIndex = newIndex;

            float newX = Dimensions.fieldMarginLeft +
                    (Dimensions.tileSize + Dimensions.spacing) * (mIndex % Settings.gameWidth);
            float newY = Dimensions.fieldMarginTop +
                    (Dimensions.tileSize + Dimensions.spacing) * (mIndex / Settings.gameWidth);

            if (Settings.animations) {
                // задание значений анимации
                if (mTranslateXAnimator.isRunning()) {
                    mTranslateXAnimator.cancel();
                }
                if (mTranslateYAnimator.isRunning()) {
                    mTranslateYAnimator.cancel();
                }

                mTranslateXAnimator.setFloatValues(mCanvasX, newX);
                mTranslateYAnimator.setFloatValues(mCanvasY, newY);

                // задание длительности
                mTranslateXAnimator.setDuration(Settings.tileAnimDuration);
                mTranslateYAnimator.setDuration(Settings.tileAnimDuration);

                // старт анимации
                mTranslateXAnimator.start();
                mTranslateYAnimator.start();
            } else {
                // обновление позиции плитки
                setCanvasX(newX);
                setCanvasY(newY);
            } // if animations

            return true;

        } // if index

        return false;
    }

    public void animateAppearance(int delay) {
        setScale(0);
        if (mScaleAnimator.isRunning()) {
            mScaleAnimator.cancel();
        }
        mScaleAnimator.setDuration(Settings.tileAnimDuration);
        mScaleAnimator.setFloatValues(0, 1);
        mScaleAnimator.setStartDelay(delay);
        mScaleAnimator.start();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Workaround бага в 6м Андроиде
    // Аниматоры пытаются сослаться/обновить состояние для мертвого объекта
    ///////////////////////////////////////////////////////////////////////////

    private final static Tile NO_OP_TILE = new Tile(0, 0) {
        @Override
        public void setCanvasX(float newX) {
        }

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public int getIndex() {
            return -1;
        }

        @Override
        public boolean at(float x, float y) {
            return false;
        }

        @Override
        public void setCanvasY(float newY) {
        }

        @Override
        public void setScale(float scale) {
        }

        @Override
        public boolean onClick() {
            return false;
        }

        @Override
        public void animateAppearance(int delay) {
        }
    };

    public void recycle() {
        mTranslateXAnimator.setTarget(NO_OP_TILE);
        mTranslateXAnimator.cancel();

        mTranslateYAnimator.setTarget(NO_OP_TILE);
        mTranslateYAnimator.cancel();

        mScaleAnimator.setTarget(NO_OP_TILE);
        mScaleAnimator.cancel();
    }

}