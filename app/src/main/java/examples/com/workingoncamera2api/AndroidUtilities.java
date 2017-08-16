package examples.com.workingoncamera2api;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;

import java.io.File;

/**
 * Created by 2114 on 05-01-2017.
 */

public class AndroidUtilities
{
    public static float largeTextSize(Context context)
    {
        float textSize = 0;

        TypedValue typedValue = new TypedValue();

        context.getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, typedValue, true);

        int[] textSizeAttribute = new int[] {android.R.attr.textSize};

        int indexOfAttributeTextSize = 0;

        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, textSizeAttribute);

        float deviceDensity = context.getResources().getDisplayMetrics().density;

        textSize =  ( ( ( typedArray.getDimensionPixelSize( indexOfAttributeTextSize, -1 ) ) / deviceDensity ) );

        typedArray.recycle();

        return textSize;
    }

    public static float mediumTextSize(Context context)
    {
        float textSize = 0;

        TypedValue typedValue = new TypedValue();

        context.getTheme().resolveAttribute(android.R.attr.textAppearanceMedium, typedValue, true);

        int[] textSizeAttribute = new int[] {android.R.attr.textSize};

        int indexOfAttributeTextSize = 0;

        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, textSizeAttribute);

        float deviceDensity = context.getResources().getDisplayMetrics().density;

        textSize =  ( ( ( typedArray.getDimensionPixelSize( indexOfAttributeTextSize, -1 ) ) / deviceDensity ) );

        typedArray.recycle();

        return textSize;
    }

    public static float smallTextSize(Context context)
    {
        float textSize = 0;

        TypedValue typedValue = new TypedValue();

        context.getTheme().resolveAttribute(android.R.attr.textAppearanceSmall, typedValue, true);

        int[] textSizeAttribute = new int[] {android.R.attr.textSize};

        int indexOfAttributeTextSize = 0;

        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data, textSizeAttribute);

        float deviceDensity = context.getResources().getDisplayMetrics().density;

        textSize =  ( ( ( typedArray.getDimensionPixelSize( indexOfAttributeTextSize, -1 ) ) / deviceDensity ) );

        typedArray.recycle();

        return textSize;
    }

    public static int pixelsToDip( int pixels, Context context )
    {
        int dp = 0;

        final float densityScale = context.getResources().getDisplayMetrics().density;
        dp = (int) (pixels * densityScale + 0.5f);
        return dp;
    }

    public static int dipToPixels( float dip, Context context )
    {
        int pixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                context.getResources().getDisplayMetrics()
        );
        return pixels;
    }

    public static Bitmap optimizedBitmap(File imageFile, int requiredWidth, int requiredHeight )
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
    }

    public static Bitmap optimizedBitmap( Context context, int resourceId, int requiredWidth, int requiredHeight )
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(context.getResources(), resourceId, options);
    }

    public static int calculateInSampleSize( BitmapFactory.Options options, int requiredWidth, int requiredHeight )
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > requiredHeight || width > requiredWidth)
        {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > requiredHeight && (halfWidth / inSampleSize) > requiredWidth)
            {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int deviceScreenWidthInPixels(Context context)
    {
        return ( context.getResources().getDisplayMetrics().widthPixels );
    }

    public static int deviceScreenHeightInPixels(Context context)
    {
        return ( context.getResources().getDisplayMetrics().heightPixels );
    }
}
