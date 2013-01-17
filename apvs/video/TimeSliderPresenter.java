package ch.cern.atlas.apvs.client.video;

import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.kiouri.sliderbar.client.presenter.Presenter;
import com.kiouri.sliderbar.client.presenter.SliderBarCalculator;

public class TimeSliderPresenter extends Presenter {

	public TimeSliderPresenter() {
		super(Orientation.HORIZONTAL);
		
		sliderBarCalulator = new MySliderBarCalculator();
	}
	
	public void setValue(int value, boolean fireEvent){
		value = checkValue(value);
		currentValue = value;
		if (!display.isAttached()){
			return;
		}		
		int absPosition  = ((MySliderBarCalculator)sliderBarCalulator).clcAbsPositionByValue(value); 
		setDragPosition(absPosition, fireEvent);
	}
	
	protected void onDragMouseMove(MouseMoveEvent event){
		if (this.maxValue == 0){
			return;
		}
		event.preventDefault();
		if (! inDrag){
			return;
		}
		int newTochPosition = display.getScaleTouchPosition(event);
		setDragPosition(sliderBarCalulator.checkAbsPosition(display.getDragPosition() +  newTochPosition - touchPosition), true);
		touchPosition = newTochPosition;		
	}

	
	private class MySliderBarCalculator extends SliderBarCalculator {
		// Note make this method accessible
		@Override
		protected int clcAbsPositionByValue(int value) {
			return super.clcAbsPositionByValue(value);
		}
	}
}
