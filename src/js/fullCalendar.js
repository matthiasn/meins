import React from 'react';
import FullCalendar from 'rc-calendar/lib/FullCalendar';
import 'rc-select/assets/index.css';
import Select from 'rc-select';
import 'moment/locale/en-gb';

const MyCalendar = props => {
    let CellRender = React.createFactory(props.cellRender);
    return (
        <div>
            <FullCalendar
                Select={Select}
                type={"date"}
                fullscreen={false}
                dateCellRender={CellRender}
                onSelect={props.onSelect}
            />
        </div>
    );
};

export default MyCalendar;
