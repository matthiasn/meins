import React, {Component} from 'react';
import {SingleDatePicker} from 'react-dates';
import 'react-dates/lib/css/_datepicker.css';

function isSameDay(a, b) {
    if (!moment.isMoment(a) || !moment.isMoment(b)) return false;
    return a.isSame(b, 'day');
}

export default class Calendar extends Component {
    state = {};

    render() {
        const onDateChange = date => {
            this.setState({ date });
            this.props.selectDate(date);
        };
        const highlighted = day1 => this.props.briefings.some(day2 => isSameDay(day1, day2));

        return (
            <div>
                <SingleDatePicker
                    date={this.state.date}
                    placeholder={"briefing"}
                    isOutsideRange={() => false}
                    isDayHighlighted={highlighted}
                    onDateChange={onDateChange}
                    focused={this.state.focused}
                    onFocusChange={({ focused }) => this.setState({ focused })}
                />
            </div>
        );
    }
}
