import { Button, Input, message, Select } from 'antd';
import Form from 'antd/lib/form/Form';
import FormItem from 'antd/lib/form/FormItem';
import React from 'react';
import { ping } from '../../api/api';



const Overview: React.FC = () => {
  return (
    <div>
      <FormItem
        label={"Titel"}
        name={"title"}>
        <Input placeholder="Name, Beschreibung..." />
      </FormItem>
      <Form>
        <FormItem
          label={"Studiengang"}
          name={"course"}>
          <Select>
            {[["winf","Wirtschaftsinformatik"], ["bwl","Betriebswirtschaftslehre"]].map(course => 
              (<Select.Option value={course[0]}>{course[1]}</Select.Option>)
              )
            }
          </Select>
        </FormItem>
        <FormItem
          label={"Modul"}
          name={"module"}>
            <Select>
              {[["mod1","Module1"], ["mod2","Module2"]].map(module => 
                (<Select.Option value={module[0]}>{module[1]}</Select.Option>)
                )
              }
          </Select>
        </FormItem>

      </Form>
      <Button onClick={e => {
        ping().then(
          res => message.success(res),
          err => message.error("Axios error")
        );
      }}>
        Ping backend
      </Button>
    </div>

  )
};

export default Overview;