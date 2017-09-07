# RemoteControllerDemo
遥控器view的初次提交
###HI,一辆开往幼儿园的小车,即将到站.

###昨天偶然看见群里哥们,抛出一张效果图,蛮有意思的,就自己实现下.

![这里写图片描述](http://img.blog.csdn.net/20170907110511837?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvZ2l2ZW1lYWNvbmRvbQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)


###遥控器的面板主控键

###看下我们临摹的效果

![这里写图片描述](http://img.blog.csdn.net/20170907110701958?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvZ2l2ZW1lYWNvbmRvbQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

###模拟器配色有点淡,这些都是自定义属性可以设置的.


###这个View用传说中的不规则点击据说很简单,但是我没去搜,我就是用两三个简单的API实现了,没啥技术含量,但是蛮有意思的.里面有一个小坑.下面用代码说下.



###实现思路

 1. 分析效果图view的组成部分,view拆分
 2. 抽取可扩展的自定义属性
 3. 测试 绘制
 4. 暴露监听给调用者



###第一步(没什么可说的)

```
 <declare-styleable name="RemoteControllerView">
        <attr name="rcv_text_color" format="color" />
        <attr name="rcv_shadow_color" format="color" />
        <attr name="rcv_stroke_color" format="color" />
        <attr name="rcv_stroke_width" format="dimension" />
        <attr name="rcv_text_size" format="dimension" />
        <attr name="rcv_oval_degree" format="integer" />
    </declare-styleable>
```

###第二步获取自定义属性,确定测量大小

```
 @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerPoint = new Point(w / 2, h / 2);

        ovalPaths = new ArrayList<>();
        ovalRegions = new ArrayList<>();
        ovalPaints = new ArrayList<>();

        rcvViewWidth = w;
        rcvViewHeight = h;
        rcvPadding = (int) (Math.min(w, h) * SCALE_OF_PADDING);
        viewContentWidht = rcvViewWidth - rcvPadding;
        viewContentHeight = rcvViewHeight - rcvPadding;
```

###第三步骤(绘制几个API.一顿draw)

```
// 画布平移到中心点改变坐标系
 canvas.translate(centerPoint.x, centerPoint.y);
 // 绘制最外层的圆环
        canvas.drawCircle(0, 0, Math.min(rcvViewWidth, rcvViewHeight) / 2, rcvStrokePaint);
        // 核心,扇形的组成的遥控器面板圆圈
        for (int i = 0; i < ovalRegions.size(); i++) {
            canvas.drawPath(ovalPaths.get(i), ovalPaints.get(i));
        }
        // 内部的小圆圈
        canvas.drawCircle(0, 0, Math.min(rcvViewWidth, rcvViewHeight) * SCALE_OF_SMALL_CIRCLE / 2, rcvWhitePaint);
        canvas.drawCircle(0, 0, Math.min(rcvViewWidth, rcvViewHeight) * SCALE_OF_SMALL_CIRCLE / 2, rcvStrokePaint);
        // 文案
        canvas.drawText("OK", textPointInView.x, textPointInView.y, rcvTextPaint);
```

###刚开始我说的坑就是这里,绘制的时候,不要让画布去旋转你设定的初始角度,因为我们这里对扇形区域分别暴露了点击事件,以及背景的选择状态色,如果旋转画布会给你视觉效果有点击A区域,B区域变色的效果,其实是假象,变色的全是是A区域,因为你旋转了画布,所以他不在原本的区域位置上了..

###一个简单的草图,不会用画图工具,凑乎看吧
![这里写图片描述](http://img.blog.csdn.net/20170907112133728?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvZ2l2ZW1lYWNvbmRvbQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

左图是不旋转的时候,ABCD四个区域,右图是选择过后的,那么ABCD必然不在原来的位置上了,
是不是豁然开朗了,如果还是不豁然......好吧,忽略.我也不知道咋说了.

所以我们绘制扇形区域拼接成圆圈的时候就要从他的startAngele开始下手了.

于是出现了下面一段垃圾代码,希望老铁能帮我写一个公式...

```
  // 注意外环的线宽占用的尺寸,这个是绘制扇形的时候限制它绘制区域的
        ovalRectF = new RectF(-rcvViewWidth / 2 + rcvStrokeWidth, -rcvViewWidth / 2 + rcvStrokeWidth, rcvViewHeight / 2 - rcvStrokeWidth, rcvViewHeight / 2 - rcvStrokeWidth);

        for (int i = 0; i < 4; i++) {
            Region tempRegin = new Region();
            Path tempPath = new Path();

            float tempStarAngle = 0;
            float tempSweepAngle;
            if (i % 2 == 0) {
                tempSweepAngle = rcvDegree;
            } else {
                tempSweepAngle = rcvOtherDegree;
            }
            // 计算扇形的开始角度,这里不能用canvas旋转的方法
            // 因为设计到扇形点击,如果画布旋转,会因为角度问题,导致感官上看上去点击错乱的问题,
            // 其实点击的区域是正确的,就是因为旋转角度导致的,注意,

            // 这块需要一个n的公式,本人没学历不会总结通用公式.....
            switch (i) {
                case 0:
                    tempStarAngle = -rcvDegree / 2;
                    break;
                case 1:
                    tempStarAngle = rcvDegree / 2;
                    break;
                case 2:
                    tempStarAngle = rcvDegree / 2 + rcvOtherDegree;
                    break;
                case 3:
                    tempStarAngle = rcvDegree / 2 + rcvOtherDegree + rcvDegree;
                    break;

            }

            tempPath.moveTo(0, 0);
            tempPath.lineTo(viewContentWidht / 2, 0);
            tempPath.addArc(ovalRectF, tempStarAngle, tempSweepAngle);
            tempPath.lineTo(0, 0);
            tempPath.close();
            RectF tempRectF = new RectF();
            tempPath.computeBounds(tempRectF, true);
            tempRegin.setPath(tempPath, new Region((int) tempRectF.left, (int) tempRectF.top, (int) tempRectF.right, (int) tempRectF.bottom));


            ovalPaths.add(tempPath);
            ovalRegions.add(tempRegin);
            ovalPaints.add(creatPaint(Color.WHITE, 0, Paint.Style.FILL, 0));
        }
```

###第四步确认点击事件区域

```
  @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x;
        float y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX() - centerPoint.x;
                y = event.getY() - centerPoint.y;
				// 这块就没啥好说了,按下去的时候用regin判断该点在哪个区域
                for (int i = 0; i < ovalRegions.size(); i++) {
                    Region tempRegin = ovalRegions.get(i);
                    boolean contains = tempRegin.contains((int) x, (int) y);
                    if (contains) {
                        seleced = i;
                    }
                }
                resetPaints();
                // 给对应的区域设置背景色
                ovalPaints.get(seleced).setColor(rcvShadowColor);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                resetPaints();
                invalidate();
                // 抬起的时候,透漏点击事件
                remoteClickAction();
                break;

        }
        return true;
    }

```

###我们这样用即可

```
 RemoteControllerView remoteControllerView = (RemoteControllerView) findViewById(R.id.rcv_view);
        remoteControllerView.setRemoteControllerClickListener(new RemoteControllerView.OnRemoteControllerClickListener() {
            @Override
            public void topClick() {
                Toast.makeText(MainActivity.this, "topClick", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void leftClick() {
                Toast.makeText(MainActivity.this, "leftClick", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void rightClick() {
                Toast.makeText(MainActivity.this, "rightClick", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void bottomClick() {
                Toast.makeText(MainActivity.this, "bottomClick", Toast.LENGTH_SHORT).show();
            }
        });
```



![这里写图片描述](http://img.blog.csdn.net/20170907113135469?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvZ2l2ZW1lYWNvbmRvbQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

