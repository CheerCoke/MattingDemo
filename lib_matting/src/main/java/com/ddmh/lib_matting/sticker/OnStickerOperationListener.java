package com.ddmh.lib_matting.sticker;

import androidx.annotation.NonNull;

public abstract class OnStickerOperationListener {
       public void onStickerAdded(@NonNull Sticker sticker){}

        public  void onStickerClicked(@NonNull Sticker sticker){}

        public void onStickerDeleted(@NonNull Sticker sticker){}

        public void onStickerDragFinished(@NonNull Sticker sticker){}

        public  void onStickerDragging(@NonNull Sticker sticker){}

        public void onStickerTouchedDown(@NonNull Sticker sticker){}

        public  void onStickerZoomFinished(@NonNull Sticker sticker){}

        public void onStickerFlipped(@NonNull Sticker sticker){}

        public void onStickerDoubleTapped(@NonNull Sticker sticker){}

    public void onStickerRotated(Sticker handlingSticker) {

    }
}