import { Avatar, Divider, List, Menu, message, Skeleton } from 'antd';
import { UserOutlined, TeamOutlined, BookOutlined } from '@ant-design/icons';
import Title from 'antd/lib/typography/Title';
import React, { useEffect, useState } from 'react';
import { CourseWithTitleAndSpecialisations } from '../../types/Course';
import { getCoursesWithTitleAndSpecialisations, getDirectorsWithNameAndMailAndId, getRequestError, getStudentsWithNameAndMailAndId, getUsersWithNameAndMailAndId } from '../../api/api';
import { getErrorMessageString } from '../../types/RequestError';
import { UserWithMailAndNameAndId } from '../../types/User';

const AdminOverview: React.FC = () => {
    const [current, setCurrent] = useState('courses');
    const [courses, setCourses] = useState<CourseWithTitleAndSpecialisations[]>([]);
    const [students, setStudents] = useState<UserWithMailAndNameAndId[]>([]);
    const [directors, setDirectors] = useState<UserWithMailAndNameAndId[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        getCoursesWithTitleAndSpecialisations().then(courses => {
            console.log(courses);
            setCourses(courses);
        }, err => {
            message.error(getErrorMessageString(getRequestError(err).errorCode))
        });
        setLoading(true)
        getStudentsWithNameAndMailAndId().then(students => {
            setLoading(false)
            console.log(students);
            setStudents(students);
        }, err => {
            setLoading(false)
            message.error(getErrorMessageString(getRequestError(err).errorCode))
        })
        getDirectorsWithNameAndMailAndId().then(directors => {
            console.log(directors);
            setDirectors(directors);
        }, err => {
            message.error(getErrorMessageString(getRequestError(err).errorCode))
        })
    }, []);

    const handleClick = (e: any) => {
        setCurrent(e.key)
    }
    return (
        <>
            <Title level={1}>
                Übersicht Administrator
            </Title>
            <Divider />
            <Menu onClick={handleClick} selectedKeys={[current]} mode="horizontal">
                <Menu.Item key="courses" icon={<BookOutlined />}>
                    Studiengänge
                </Menu.Item>
                <Menu.Item key="directors" icon={<UserOutlined />}>
                    Studiengangsleiter
                </Menu.Item>
                <Menu.Item key="students" icon={<TeamOutlined />}>
                    Studenten
                </Menu.Item>
            </Menu>
            <Divider />
            {current === "courses" && <>
                <List
                    className="students-list"
                    itemLayout="horizontal"
                    loading={loading}
                    dataSource={courses}
                    renderItem={course => (
                        <List.Item
                            actions={[<a key="list-loadmore-edit">edit</a>, <a key="list-loadmore-more">more</a>]}
                        >
                            <Skeleton title={false} loading={loading} active>
                                <List.Item.Meta
                                    title={course.title}
                                    description={course.id}
                                />
                                {course.specialisationCourses.map(specilisationCourse => (
                                    <List.Item title={specilisationCourse.title}/>
                                ))}
                            </Skeleton>
                        </List.Item>
                    )}
                />
            </>}
            {current === "directors" && <>
                <List
                    className="students-list"
                    itemLayout="horizontal"
                    loading={loading}
                    dataSource={directors}
                    renderItem={director => (
                        <List.Item
                            actions={[<a key="list-loadmore-edit">edit</a>, <a key="list-loadmore-more">more</a>]}
                        >
                            <Skeleton avatar title={false} loading={loading} active>
                                <List.Item.Meta
                                    title={director.firstName + " " + director.lastName}
                                    description={director.email}
                                />
                                <div>content</div>
                            </Skeleton>
                        </List.Item>
                    )}
                />
            </>}
            {current === "students" && <>
                <List
                    className="students-list"
                    itemLayout="horizontal"
                    loading={loading}
                    dataSource={students}
                    renderItem={student => (
                        <List.Item
                            actions={[<a key="list-loadmore-edit">edit</a>, <a key="list-loadmore-more">more</a>]}
                        >
                            <Skeleton avatar title={false} loading={loading} active>
                                <List.Item.Meta
                                    title={student.firstName + " " + student.lastName}
                                    description={student.email}
                                />
                                <div>content</div>
                            </Skeleton>
                        </List.Item>
                    )}
                />
            </>}

        </>
    )
};

export default AdminOverview;