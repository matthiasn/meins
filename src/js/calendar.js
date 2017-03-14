import Calendar from 'rc-calendar';
import React from 'react';

const MyCalendar = props => (
    <div>
        <Calendar
            onSelect={props.onSelect}
            showDateInput={false}
            showToday={false}
        />
    </div>
);

export default MyCalendar;
