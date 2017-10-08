import {Component} from 'react';
import BigCalendar from 'react-big-calendar';
import moment from 'moment';

BigCalendar.momentLocalizer(moment);

let allViews = ['day', 'week'];

class MyEvent extends Component {
    render() {
        return (
            <div onClick={this.props.event.click}>
                {this.props.event.title}
            </div>
        )
    }
}

const eventPropGetter = (event, start, end, isSelected) => {
    return {
        style: {backgroundColor: event.color}
    }
};

let components = {
    event: MyEvent
};

let messages = {
    allDay: ''
};

export default class Calendar extends Component {
    render() {
        return (
            <BigCalendar
                {...this.props}
                events={this.props.events}
                views={allViews}
                defaultView='day'
                toolbar={false}
                components={components}
                messages={messages}
                eventPropGetter={eventPropGetter}
                defaultDate={this.props.defaultDate}
                scrollToTime={this.props.scrollToDate}
            />
        )
    }
}
