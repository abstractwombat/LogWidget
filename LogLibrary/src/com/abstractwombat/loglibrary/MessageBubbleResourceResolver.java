package com.abstractwombat.loglibrary;

import android.content.Context;
import android.graphics.Color;

/**
 * Created by Mike on 3/8/2016.
 */
public class MessageBubbleResourceResolver {

    public static int getResource(Context context, int baseResource, int color) {
        String styleName = resourceIdToName(context, baseResource);
        String colorSuffix = getColorResourceSuffix(color);

        return nameToDrawableResourceId(context, styleName + colorSuffix);
    }

    public static int getBaseResource(Context context, int resource){
        String styleName = resourceIdToName(context, resource);
        String shortened = styleName.substring(0, styleName.lastIndexOf("_"));

        int res = nameToDrawableResourceId(context, shortened);
        if (res > 0){
            return res;
        }else{
            return nameToDrawableResourceId(context, styleName);
        }
    }

    public static int getColor(Context context, int resource){
        String styleName = resourceIdToName(context, resource);
        String colorSuffix = styleName.substring(styleName.lastIndexOf("_")+1, styleName.length());
        return getColor(colorSuffix);
    }


    private static String resourceIdToName(Context context, int baseResource) {
        return context.getResources().getResourceEntryName(baseResource);
    }

    private static int nameToDrawableResourceId(Context context, String s) {
        String uri = "drawable/" + s;
        return context.getResources().getIdentifier(uri, null, context.getPackageName());
    }

    private static String getColorResourceSuffix(Integer color) {
        String colorSuffix = "";
        if (color.equals(Color.parseColor("#FFFFFF"))) {
            colorSuffix = "";
        } else if (color.equals(Color.parseColor("#F44336"))) {
            colorSuffix = "_red";
        } else if (color.equals(Color.parseColor("#E91E63"))) {
            colorSuffix = "_pink";
        } else if (color.equals(Color.parseColor("#9C27B0"))) {
            colorSuffix = "_purple";
        } else if (color.equals(Color.parseColor("#673AB7"))) {
            colorSuffix = "_deeppurple";
        } else if (color.equals(Color.parseColor("#3F51B5"))) {
            colorSuffix = "_indigo";
        } else if (color.equals(Color.parseColor("#2196F3"))) {
            colorSuffix = "_blue";
        } else if (color.equals(Color.parseColor("#03A9F4"))) {
            colorSuffix = "_lightblue";
        } else if (color.equals(Color.parseColor("#00BCD4"))) {
            colorSuffix = "_cyan";
        } else if (color.equals(Color.parseColor("#009688"))) {
            colorSuffix = "_teal";
        } else if (color.equals(Color.parseColor("#4CAF50"))) {
            colorSuffix = "_green";
        } else if (color.equals(Color.parseColor("#8BC34A"))) {
            colorSuffix = "_lightgreen";
        } else if (color.equals(Color.parseColor("#CDDC39"))) {
            colorSuffix = "_lime";
        } else if (color.equals(Color.parseColor("#FFEB3B"))) {
            colorSuffix = "_yellow";
        } else if (color.equals(Color.parseColor("#FFC107"))) {
            colorSuffix = "_amber";
        } else if (color.equals(Color.parseColor("#FF9800"))) {
            colorSuffix = "_orange";
        } else if (color.equals(Color.parseColor("#FF5722"))) {
            colorSuffix = "_deeporange";
        } else if (color.equals(Color.parseColor("#795548"))) {
            colorSuffix = "_brown";
        } else if (color.equals(Color.parseColor("#9E9E9E"))) {
            colorSuffix = "_grey";
        } else if (color.equals(Color.parseColor("#607D8B"))) {
            colorSuffix = "_bluegrey";
        } else if (color.equals(Color.parseColor("#000000"))) {
            colorSuffix = "_black";
        }
        return colorSuffix;
    }

    private static int getColor(String colorSuffix) {
        int color = Color.parseColor("#FFFFFF");
        switch (colorSuffix) {
            case "":
                color = Color.parseColor("#FFFFFF");
                break;
            case "_red":
                color = Color.parseColor("#F44336");
                break;
            case "_pink":
                color = Color.parseColor("#E91E63");
                break;
            case "_purple":
                color = Color.parseColor("#9C27B0");
                break;
            case "_deeppurple":
                color = Color.parseColor("#673AB7");
                break;
            case "_indigo":
                color = Color.parseColor("#3F51B5");
                break;
            case "_blue":
                color = Color.parseColor("#2196F3");
                break;
            case "_lightblue":
                color = Color.parseColor("#03A9F4");
                break;
            case "_cyan":
                color = Color.parseColor("#00BCD4");
                break;
            case "_teal":
                color = Color.parseColor("#009688");
                break;
            case "_green":
                color = Color.parseColor("#4CAF50");
                break;
            case "_lightgreen":
                color = Color.parseColor("#8BC34A");
                break;
            case "_lime":
                color = Color.parseColor("#CDDC39");
                break;
            case "_yellow":
                color = Color.parseColor("#FFEB3B");
                break;
            case "_amber":
                color = Color.parseColor("#FFC107");
                break;
            case "_orange":
                color = Color.parseColor("#FF9800");
                break;
            case "_deeporange":
                color = Color.parseColor("#FF5722");
                break;
            case "_brown":
                color = Color.parseColor("#795548");
                break;
            case "_grey":
                color = Color.parseColor("#9E9E9E");
                break;
            case "_bluegrey":
                color = Color.parseColor("#607D8B");
                break;
            case "_black":
                color = Color.parseColor("#000000");
                break;
        }
        return color;
    }

}
